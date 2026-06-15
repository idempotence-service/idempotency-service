package ru.itmo.idempotency.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.core.domain.EventAuditEntity;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.EventAuditRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;
import ru.itmo.idempotency.core.web.ManualReviewDtos;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManualReviewService {

    private final IdempotencyRepository idempotencyRepository;
    private final EventAuditRepository eventAuditRepository;
    private final IdempotencySearchService idempotencySearchService;
    private final IdempotencyService idempotencyService;
    private final StorageShardExecutor storageShardExecutor;

    @Transactional(readOnly = true)
    public Page<ManualReviewDtos.ErrorEventItem> getErrorEvents(int page, int limit, Sort.Direction direction) {
        int fetchSize = Math.max(limit, (page + 1) * limit);
        Sort sort = Sort.by(direction, "createDate");
        List<IdempotencyEntity> items = storageShardExecutor.collectFromAllReadOnly(shardId -> idempotencyRepository.findByStatus(
                IdempotencyStatus.ERROR,
                PageRequest.of(0, fetchSize, sort)
        ).getContent());

        Comparator<IdempotencyEntity> comparator = Comparator.comparing(IdempotencyEntity::getCreateDate)
                .thenComparing(IdempotencyEntity::getGlobalKey);
        if (direction == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }

        List<ManualReviewDtos.ErrorEventItem> content = items.stream()
                .sorted(comparator)
                .skip((long) page * limit)
                .limit(limit)
                .map(this::toErrorEventItem)
                .toList();
        long total = storageShardExecutor.sumLongReadOnly(shardId -> idempotencyRepository.countByStatus(IdempotencyStatus.ERROR));

        return new PageImpl<>(content, PageRequest.of(page, limit, sort), total);
    }

    @Transactional
    public String restart(String globalKey) {
        IdempotencyEntity entity = idempotencySearchService.acquireUniqueWaitIfLocked(globalKey)
                .orElseThrow(() -> new EventNotFoundException("Задача с данными " + globalKey + " не найдена"));
        if (entity.getStatus() != IdempotencyStatus.ERROR) {
            return "Задача уже перезапущена";
        }
        idempotencyService.restart(entity);
        return "Задача успешно перезапущена";
    }

    @Transactional(readOnly = true)
    public ManualReviewDtos.EventDetails getByGlobalKey(String globalKey) {
        IdempotencyEntity entity = idempotencySearchService.findByGlobalKey(globalKey)
                .orElseThrow(() -> new EventNotFoundException("Событие с данными " + globalKey + " не найдено"));
        return new ManualReviewDtos.EventDetails(
                entity.getGlobalKey(),
                entity.getSourceUid(),
                entity.getServiceName(),
                entity.getIntegrationName(),
                entity.getStatus().name(),
                entity.getStatusDescription(),
                entity.getCreateDate(),
                entity.getUpdateDate(),
                entity.getPayload(),
                entity.getHeaders()
        );
    }

    private ManualReviewDtos.ErrorEventItem toErrorEventItem(IdempotencyEntity entity) {
        return new ManualReviewDtos.ErrorEventItem(
                entity.getStatus().name(),
                entity.getGlobalKey(),
                entity.getServiceName(),
                entity.getIntegrationName()
        );
    }

    @Transactional(readOnly = true)
    public Page<ManualReviewDtos.DuplicateEventItem> getDuplicateEvents(int page, int limit) {
        int fetchSize = Math.max(limit, (page + 1) * limit);
        Sort sort = Sort.by(Sort.Direction.DESC, "createDate");
        List<EventAuditEntity> items = storageShardExecutor.collectFromAllReadOnly(
                shardId -> eventAuditRepository.findByReason(
                        AuditReasons.IDEMPOTENCY_FAILED,
                        PageRequest.of(0, fetchSize, sort)));

        List<ManualReviewDtos.DuplicateEventItem> content = items.stream()
                .sorted(Comparator.comparing(EventAuditEntity::getCreateDate).reversed()
                        .thenComparing(EventAuditEntity::getGlobalKey))
                .skip((long) page * limit)
                .limit(limit)
                .map(entity -> new ManualReviewDtos.DuplicateEventItem(
                        entity.getGlobalKey(),
                        entity.getServiceName(),
                        entity.getIntegrationName(),
                        entity.getReason(),
                        entity.getCreateDate()
                ))
                .toList();
        long total = storageShardExecutor.sumLongReadOnly(
                shardId -> eventAuditRepository.countByReason(AuditReasons.IDEMPOTENCY_FAILED));

        return new PageImpl<>(content, PageRequest.of(page, limit, sort), total);
    }

    @Transactional(readOnly = true)
    public long getDuplicateCount() {
        return storageShardExecutor.sumLongReadOnly(shardId -> eventAuditRepository.countByReason(AuditReasons.IDEMPOTENCY_FAILED));
    }

    @Transactional(readOnly = true)
    public long getTimeoutCount() {
        return storageShardExecutor.sumLongReadOnly(
                shardId -> eventAuditRepository.countDistinctGlobalKeyByReason(AuditReasons.ASYNC_REPLY_TIMEOUT));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getAuditActivitySince(OffsetDateTime since) {
        Map<String, Long> aggregated = new HashMap<>();
        storageShardExecutor.collectFromAllReadOnly(
                shardId -> since == null 
                    ? eventAuditRepository.countByReason() 
                    : eventAuditRepository.countByReasonSince(since))
                .forEach(row -> {
                    String reason = (String) row[0];
                    Long count = (Long) row[1];
                    aggregated.merge(reason, count, Long::sum);
                });
        return aggregated;
    }
}

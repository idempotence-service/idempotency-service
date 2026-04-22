package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.domain.ProcessingResult;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KafkaEventOutboxService {

    private final KafkaEventOutboxRepository kafkaEventOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public KafkaEventOutboxEntity save(String globalKey,
                                       RouteModels.RouteSnapshot snapshot,
                                       ProcessingResult result,
                                       String resultDescription) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return kafkaEventOutboxRepository.save(KafkaEventOutboxEntity.builder()
                .globalKey(globalKey)
                .serviceName(snapshot.service())
                .integrationName(snapshot.integration())
                .yamlSnapshot(objectMapper.valueToTree(snapshot))
                .status(OutboxStatus.NEW)
                .result(result)
                .resultDescription(DescriptionUtils.limit(resultDescription))
                .retryCount(0)
                .nextAttemptDate(now)
                .build());
    }

    @Transactional
    public Optional<KafkaEventOutboxEntity> claimNextNew(String ownerId, Duration leaseDuration) {
        return kafkaEventOutboxRepository.lockFirstAvailableByStatus(OutboxStatus.NEW.name())
                .map(entity -> claim(entity, ownerId, leaseDuration));
    }

    @Transactional
    public KafkaEventOutboxEntity changeStatus(KafkaEventOutboxEntity entity, OutboxStatus status, String description) {
        applyStatus(entity, status, description);
        return kafkaEventOutboxRepository.save(entity);
    }

    @Transactional
    public KafkaEventOutboxEntity markAsError(KafkaEventOutboxEntity entity, String description) {
        return changeStatus(entity, OutboxStatus.ERROR, description);
    }

    @Transactional
    public KafkaEventOutboxEntity scheduleRetry(KafkaEventOutboxEntity entity,
                                                String description,
                                                Duration delay,
                                                int maxAttempts) {
        applyRetry(entity, description, delay, maxAttempts);
        return kafkaEventOutboxRepository.save(entity);
    }

    @Transactional
    public boolean completeClaimedDispatch(Long id, String ownerId, String description) {
        return updateClaimed(id, ownerId, entity -> applyStatus(entity, OutboxStatus.DONE, description));
    }

    @Transactional
    public boolean retryClaimedDispatch(Long id,
                                        String ownerId,
                                        String description,
                                        Duration delay,
                                        int maxAttempts) {
        return updateClaimed(id, ownerId, entity -> applyRetry(entity, description, delay, maxAttempts));
    }

    @Transactional
    public boolean failClaimedDispatch(Long id, String ownerId, String description) {
        return updateClaimed(id, ownerId, entity -> applyStatus(entity, OutboxStatus.ERROR, description));
    }

    private KafkaEventOutboxEntity claim(KafkaEventOutboxEntity entity, String ownerId, Duration leaseDuration) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setOwnerId(ownerId);
        entity.setLeaseUntil(now.plus(leaseDuration));
        entity.setLastClaimDate(now);
        return kafkaEventOutboxRepository.save(entity);
    }

    private boolean updateClaimed(Long id,
                                  String ownerId,
                                  java.util.function.Consumer<KafkaEventOutboxEntity> updater) {
        KafkaEventOutboxEntity entity = kafkaEventOutboxRepository.findByIdForUpdate(id).orElse(null);
        if (entity == null || entity.getOwnerId() == null || !entity.getOwnerId().equals(ownerId)) {
            return false;
        }

        updater.accept(entity);
        kafkaEventOutboxRepository.save(entity);
        return true;
    }

    private void applyStatus(KafkaEventOutboxEntity entity, OutboxStatus status, String description) {
        entity.setStatus(status);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        clearLease(entity);
    }

    private void applyRetry(KafkaEventOutboxEntity entity,
                            String description,
                            Duration delay,
                            int maxAttempts) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int nextRetryCount = entity.getRetryCount() + 1;
        entity.setRetryCount(nextRetryCount);
        if (nextRetryCount >= maxAttempts) {
            entity.setStatus(OutboxStatus.ERROR);
            entity.setStatusDescription(DescriptionUtils.limit(limitReachedDescription(description, nextRetryCount)));
            entity.setNextAttemptDate(now);
            clearLease(entity);
            return;
        }

        entity.setStatus(OutboxStatus.NEW);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(now.plus(delay));
        clearLease(entity);
    }

    private void clearLease(KafkaEventOutboxEntity entity) {
        entity.setOwnerId(null);
        entity.setLeaseUntil(null);
    }

    private String limitReachedDescription(String description, int attempts) {
        String baseDescription = description == null || description.isBlank() ? "Повторная попытка не удалась." : description;
        return baseDescription + " Достигнут лимит повторных попыток: " + attempts;
    }
}

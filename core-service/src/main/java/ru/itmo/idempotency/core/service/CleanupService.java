package ru.itmo.idempotency.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {

    private final CoreProperties coreProperties;
    private final IdempotencySearchService idempotencySearchService;
    private final IdempotencyService idempotencyService;
    private final KafkaEventOutboxRepository kafkaEventOutboxRepository;
    private final KafkaEventOutboxService kafkaEventOutboxService;
    private final StorageShardExecutor storageShardExecutor;

    public int cleanupExpiredRecords() {
        int deleted = cleanupCommitted();
        deleted += cleanupOutbox(OutboxStatus.DONE);
        deleted += cleanupOutbox(OutboxStatus.ERROR);
        return deleted;
    }

    public int cleanupCommitted() {
        int deleted = 0;
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minus(coreProperties.getCleanup().getRetention());
        for (String shardId : storageShardExecutor.shardIdsInScanOrder()) {
            while (true) {
                List<IdempotencyEntity> batch = storageShardExecutor.runInTransactionOnShard(shardId, () ->
                        idempotencySearchService.findCommittedBatchForCleanup(
                                threshold,
                                coreProperties.getCleanup().getBatchSize()
                        ));
                if (batch.isEmpty()) {
                    break;
                }
                idempotencyService.delete(batch);
                deleted += batch.size();
            }
        }
        if (deleted > 0) {
            log.info("Deleted {} committed idempotency records", deleted);
        }
        return deleted;
    }

    private int cleanupOutbox(OutboxStatus status) {
        int deleted = 0;
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minus(coreProperties.getCleanup().getRetention());
        for (String shardId : storageShardExecutor.shardIdsInScanOrder()) {
            while (true) {
                List<KafkaEventOutboxEntity> batch = storageShardExecutor.runInTransactionOnShard(shardId, () ->
                        kafkaEventOutboxRepository.findByStatusAndUpdateDateBefore(
                                status,
                                threshold,
                                PageRequest.of(0, coreProperties.getCleanup().getBatchSize(), Sort.by("updateDate").ascending())
                        ));
                if (batch.isEmpty()) {
                    break;
                }
                kafkaEventOutboxService.delete(batch);
                deleted += batch.size();
            }
        }
        if (deleted > 0) {
            log.info("Deleted {} outbox records with status {}", deleted, status);
        }
        return deleted;
    }
}

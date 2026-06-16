package ru.itmo.idempotency.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
public class ReplyTimeoutRecoveryService {

    private final IdempotencySearchService idempotencySearchService;
    private final IdempotencyService idempotencyService;
    private final EventAuditService eventAuditService;
    private final CoreJsonSupport coreJsonSupport;
    private final CoreProperties coreProperties;
    private final CoreMetrics coreMetrics;
    private final MdcContextSupport mdcContextSupport;
    private final StorageShardExecutor storageShardExecutor;

    public ReplyTimeoutRecoveryService(IdempotencySearchService idempotencySearchService,
                                       IdempotencyService idempotencyService,
                                       EventAuditService eventAuditService,
                                       CoreJsonSupport coreJsonSupport,
                                       CoreProperties coreProperties,
                                       CoreMetrics coreMetrics,
                                       MdcContextSupport mdcContextSupport,
                                       StorageShardExecutor storageShardExecutor) {
        this.idempotencySearchService = idempotencySearchService;
        this.idempotencyService = idempotencyService;
        this.eventAuditService = eventAuditService;
        this.coreJsonSupport = coreJsonSupport;
        this.coreProperties = coreProperties;
        this.coreMetrics = coreMetrics;
        this.mdcContextSupport = mdcContextSupport;
        this.storageShardExecutor = storageShardExecutor;
    }

    public int processBatch(int batchSize) {
        int processed = 0;
        while (processed < batchSize) {
            if (!storageShardExecutor.firstTrueInTransaction(shardId -> processSingle())) {
                break;
            }
            processed++;
        }
        return processed;
    }

    private boolean processSingle() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC).minus(coreProperties.getResilience().getReplyTimeout());
        IdempotencyEntity entity = idempotencySearchService.acquireFirstTimedOutReply(threshold).orElse(null);
        if (entity == null) {
            return false;
        }

        RouteModels.RouteSnapshot snapshot = coreJsonSupport.parseSnapshot(entity.getYamlSnapshot());
        try (MdcContextSupport.Scope ignored = mdcContextSupport.open(entity.getGlobalKey(), entity.getSourceUid(), snapshot.integration(), null)) {
            idempotencyService.scheduleRetry(
                    entity,
                    "Не получен асинхронный ответ от системы-получателя за " + coreProperties.getResilience().getReplyTimeout(),
                    coreProperties.getResilience().getDeliveryRetryDelay(),
                    coreProperties.getResilience().getMaxAttempts()
            );
            eventAuditService.save(entity.getGlobalKey(), snapshot, AuditReasons.ASYNC_REPLY_TIMEOUT, entity.getHeaders(), entity.getPayload());
            coreMetrics.recordAsyncReplyTimeout(snapshot.integration());
            log.warn("Timed out waiting for async reply for {}", entity.getGlobalKey());
        }
        return true;
    }
}

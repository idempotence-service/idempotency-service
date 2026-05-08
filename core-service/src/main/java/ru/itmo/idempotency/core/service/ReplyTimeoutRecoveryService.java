package ru.itmo.idempotency.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;

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
    private final TransactionTemplate transactionTemplate;

    public ReplyTimeoutRecoveryService(IdempotencySearchService idempotencySearchService,
                                       IdempotencyService idempotencyService,
                                       EventAuditService eventAuditService,
                                       CoreJsonSupport coreJsonSupport,
                                       CoreProperties coreProperties,
                                       CoreMetrics coreMetrics,
                                       MdcContextSupport mdcContextSupport,
                                       PlatformTransactionManager transactionManager) {
        this.idempotencySearchService = idempotencySearchService;
        this.idempotencyService = idempotencyService;
        this.eventAuditService = eventAuditService;
        this.coreJsonSupport = coreJsonSupport;
        this.coreProperties = coreProperties;
        this.coreMetrics = coreMetrics;
        this.mdcContextSupport = mdcContextSupport;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public int processBatch(int batchSize) {
        int processed = 0;
        while (processed < batchSize) {
            Boolean currentProcessed = transactionTemplate.execute(status -> processSingle());
            if (!Boolean.TRUE.equals(currentProcessed)) {
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
            coreMetrics.recordAsyncReplyTimeout();
            log.warn("Timed out waiting for async reply for {}", entity.getGlobalKey());
        }
        return true;
    }
}

package ru.itmo.idempotency.core.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.repository.EventAuditRepository;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.DoubleSupplier;

@Component
public class CoreMetrics {

    private final MeterRegistry meterRegistry;
    private final CoreProperties coreProperties;
    private final IdempotencyRepository idempotencyRepository;
    private final EventAuditRepository eventAuditRepository;
    private final KafkaEventOutboxRepository kafkaEventOutboxRepository;
    private final StorageShardExecutor storageShardExecutor;
    private final RouteCatalog routeCatalog;

    public CoreMetrics(MeterRegistry meterRegistry,
                       CoreProperties coreProperties,
                       IdempotencyRepository idempotencyRepository,
                       EventAuditRepository eventAuditRepository,
                       KafkaEventOutboxRepository kafkaEventOutboxRepository,
                       StorageShardExecutor storageShardExecutor,
                       RouteCatalog routeCatalog) {
        this.meterRegistry = meterRegistry;
        this.coreProperties = coreProperties;
        this.idempotencyRepository = idempotencyRepository;
        this.eventAuditRepository = eventAuditRepository;
        this.kafkaEventOutboxRepository = kafkaEventOutboxRepository;
        this.storageShardExecutor = storageShardExecutor;
        this.routeCatalog = routeCatalog;

        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.RESERVED);
        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.COMMITTED);
        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.ERROR);
        registerOutboxBacklogGauge(meterRegistry, OutboxStatus.NEW);
        registerOutboxBacklogGauge(meterRegistry, OutboxStatus.DONE);
        registerOutboxBacklogGauge(meterRegistry, OutboxStatus.ERROR);
        Gauge.builder("idempotency.leases.expired", this, CoreMetrics::countExpiredIdempotencyLeases).register(meterRegistry);
        Gauge.builder("idempotency.outbox.leases.expired", this, CoreMetrics::countExpiredOutboxLeases).register(meterRegistry);
        Gauge.builder("idempotency.manual_review.backlog", this, CoreMetrics::countDuplicateEvents)
                .tag("type", "duplicates")
                .register(meterRegistry);
        Gauge.builder("idempotency.manual_review.backlog", this, CoreMetrics::countTimedOutEvents)
                .tag("type", "timeouts")
                .register(meterRegistry);
        Gauge.builder("idempotency.routes", routeCatalog, catalog -> catalog.getAllRoutes().size())
                .tag("state", "all")
                .register(meterRegistry);
        Gauge.builder("idempotency.routes", routeCatalog, catalog -> catalog.getEnabledRoutes().size())
                .tag("state", "enabled")
                .register(meterRegistry);

        registerGauge("idempotency.config.scheduler.delay.seconds", "kind", "outbox", () -> durationToSeconds(coreProperties.getScheduler().getOutboxFixedDelay()));
        registerGauge("idempotency.config.scheduler.delay.seconds", "kind", "delivery", () -> durationToSeconds(coreProperties.getScheduler().getDeliveryFixedDelay()));
        registerGauge("idempotency.config.scheduler.delay.seconds", "kind", "reply_timeout", () -> durationToSeconds(coreProperties.getScheduler().getReplyTimeoutFixedDelay()));
        registerGauge("idempotency.config.scheduler.delay.seconds", "kind", "cleanup", () -> durationToSeconds(coreProperties.getScheduler().getCleanupFixedDelay()));
        registerGauge("idempotency.config.scheduler.batch_size", "kind", "default", () -> coreProperties.getScheduler().getBatchSize());
        registerGauge("idempotency.config.listener.concurrency", "kind", "inbound", () -> coreProperties.getListener().getInboundConcurrency());
        registerGauge("idempotency.config.listener.concurrency", "kind", "reply", () -> coreProperties.getListener().getReplyConcurrency());
        registerGauge("idempotency.config.resilience.delay.seconds", "kind", "outbox_retry", () -> coreProperties.getResilience().getOutboxRetryDelay().toSeconds());
        registerGauge("idempotency.config.resilience.delay.seconds", "kind", "delivery_retry", () -> coreProperties.getResilience().getDeliveryRetryDelay().toSeconds());
        registerGauge("idempotency.config.resilience.delay.seconds", "kind", "reply_timeout", () -> coreProperties.getResilience().getReplyTimeout().toSeconds());
        registerGauge("idempotency.config.resilience.delay.seconds", "kind", "lease_duration", () -> coreProperties.getResilience().getLeaseDuration().toSeconds());
        registerGauge("idempotency.config.resilience.max_attempts", "kind", "default", () -> coreProperties.getResilience().getMaxAttempts());
        registerGauge("idempotency.config.cleanup.retention.seconds", "kind", "default", () -> coreProperties.getCleanup().getRetention().toSeconds());
        registerGauge("idempotency.config.cleanup.batch_size", "kind", "default", () -> coreProperties.getCleanup().getBatchSize());
    }

    public void recordInboundUnique(String integration) {
        recordCounter("idempotency.inbound.total", "result", "unique", integration);
    }

    public void recordInboundDuplicate(String integration) {
        recordCounter("idempotency.inbound.total", "result", "duplicate", integration);
    }

    public void recordInboundInvalid(String integration) {
        recordCounter("idempotency.inbound.total", "result", "invalid", integration);
    }

    public void recordAsyncReplySuccess(String integration) {
        recordCounter("idempotency.async_reply.total", "result", "success", integration);
    }

    public void recordAsyncReplyResend(String integration) {
        recordCounter("idempotency.async_reply.total", "result", "resend", integration);
    }

    public void recordAsyncReplyFailure(String integration) {
        recordCounter("idempotency.async_reply.total", "result", "failure", integration);
    }

    public void recordAsyncReplyTimeout(String integration) {
        recordCounter("idempotency.async_reply.total", "result", "timeout", integration);
    }

    public void recordAsyncReplyOrphan(String integration) {
        recordCounter("idempotency.async_reply.total", "result", "orphan", integration);
    }

    public void recordAsyncReplyInvalid(String integration) {
        recordCounter("idempotency.async_reply.total", "result", "invalid", integration);
    }

    public void recordDeliverySuccess(String integration, Duration duration) {
        recordOutcomeWithDuration("idempotency.delivery.total", "idempotency.delivery.duration", "success", integration, duration);
    }

    public void recordDeliveryRetry(String integration, Duration duration) {
        recordOutcomeWithDuration("idempotency.delivery.total", "idempotency.delivery.duration", "retry", integration, duration);
    }

    public void recordDeliveryFailure(String integration, Duration duration) {
        recordOutcomeWithDuration("idempotency.delivery.total", "idempotency.delivery.duration", "failure", integration, duration);
    }

    public void recordOutboxSuccess(String integration, Duration duration) {
        recordOutcomeWithDuration("idempotency.outbox.total", "idempotency.outbox.duration", "success", integration, duration);
    }

    public void recordOutboxRetry(String integration, Duration duration) {
        recordOutcomeWithDuration("idempotency.outbox.total", "idempotency.outbox.duration", "retry", integration, duration);
    }

    public void recordOutboxFailure(String integration, Duration duration) {
        recordOutcomeWithDuration("idempotency.outbox.total", "idempotency.outbox.duration", "failure", integration, duration);
    }

    public void recordDeliveryOwnershipLost(String integration) {
        recordCounter("idempotency.worker.ownership_lost.total", "worker", "delivery", integration);
    }

    public void recordOutboxOwnershipLost(String integration) {
        recordCounter("idempotency.worker.ownership_lost.total", "worker", "outbox", integration);
    }

    public void recordManualReviewAction(String action, String result) {
        meterRegistry.counter(
                "idempotency.manual_review.actions.total",
                "action", sanitizeTag(action),
                "result", sanitizeTag(result)
        ).increment();
    }

    public void recordConfigUpdate(String section) {
        meterRegistry.counter("idempotency.config.updates.total", "section", sanitizeTag(section)).increment();
    }

    public void recordApiError(String code) {
        meterRegistry.counter("idempotency.api.errors.total", "code", sanitizeTag(code)).increment();
    }

    private void recordCounter(String name, String tagKey, String tagValue, String integration) {
        counter(name, tagKey, tagValue, integration).increment();
    }

    private Counter counter(String name, String tagKey, String tagValue, String integration) {
        return meterRegistry.counter(
                name,
                tagKey, sanitizeTag(tagValue),
                "integration", sanitizeTag(integration)
        );
    }

    private void recordOutcomeWithDuration(String counterName,
                                           String timerName,
                                           String result,
                                           String integration,
                                           Duration duration) {
        recordCounter(counterName, "result", result, integration);
        timer(timerName, result, integration).record(duration);
    }

    private Timer timer(String name, String result, String integration) {
        return Timer.builder(name)
                .tag("result", sanitizeTag(result))
                .tag("integration", sanitizeTag(integration))
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    private void registerIdempotencyBacklogGauge(MeterRegistry meterRegistry, IdempotencyStatus status) {
        Gauge.builder("idempotency.queue.backlog", this,
                        metrics -> metrics.storageShardExecutor.sumLongReadOnly(shardId -> metrics.idempotencyRepository.countByStatus(status)))
                .tag("status", status.name())
                .register(meterRegistry);
    }

    private void registerOutboxBacklogGauge(MeterRegistry meterRegistry, OutboxStatus status) {
        Gauge.builder("idempotency.outbox.backlog", this,
                        metrics -> metrics.storageShardExecutor.sumLongReadOnly(shardId -> metrics.kafkaEventOutboxRepository.countByStatus(status)))
                .tag("status", status.name())
                .register(meterRegistry);
    }

    private double countExpiredIdempotencyLeases() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC);
        return storageShardExecutor.sumLongReadOnly(shardId -> idempotencyRepository.countExpiredLeases(threshold));
    }

    private double countExpiredOutboxLeases() {
        OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC);
        return storageShardExecutor.sumLongReadOnly(shardId -> kafkaEventOutboxRepository.countExpiredLeases(threshold));
    }

    private double countDuplicateEvents() {
        return storageShardExecutor.sumLongReadOnly(shardId -> eventAuditRepository.countByReason(AuditReasons.IDEMPOTENCY_FAILED));
    }

    private double countTimedOutEvents() {
        return storageShardExecutor.sumLongReadOnly(
                shardId -> eventAuditRepository.countDistinctGlobalKeyByReason(AuditReasons.ASYNC_REPLY_TIMEOUT));
    }

    private void registerGauge(String name, String tagKey, String tagValue, DoubleSupplier supplier) {
        Gauge.builder(name, supplier, DoubleSupplier::getAsDouble)
                .tag(tagKey, tagValue)
                .strongReference(true)
                .register(meterRegistry);
    }

    private double durationToSeconds(Duration duration) {
        return duration.toMillis() / 1000.0;
    }

    private String sanitizeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}

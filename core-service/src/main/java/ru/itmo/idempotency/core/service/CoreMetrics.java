package ru.itmo.idempotency.core.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CoreMetrics {

    private final IdempotencyRepository idempotencyRepository;
    private final KafkaEventOutboxRepository kafkaEventOutboxRepository;

    private final Counter inboundUniqueCounter;
    private final Counter inboundDuplicateCounter;
    private final Counter inboundInvalidCounter;
    private final Counter asyncReplySuccessCounter;
    private final Counter asyncReplyResendCounter;
    private final Counter asyncReplyFailureCounter;
    private final Counter asyncReplyTimeoutCounter;
    private final Counter asyncReplyOrphanCounter;
    private final Counter asyncReplyInvalidCounter;
    private final Counter deliverySuccessCounter;
    private final Counter deliveryRetryCounter;
    private final Counter deliveryFailureCounter;
    private final Counter outboxSuccessCounter;
    private final Counter outboxRetryCounter;
    private final Counter outboxFailureCounter;
    private final Counter deliveryOwnershipLostCounter;
    private final Counter outboxOwnershipLostCounter;
    private final Timer deliveryDurationTimer;
    private final Timer outboxDurationTimer;

    public CoreMetrics(MeterRegistry meterRegistry,
                       IdempotencyRepository idempotencyRepository,
                       KafkaEventOutboxRepository kafkaEventOutboxRepository) {
        this.idempotencyRepository = idempotencyRepository;
        this.kafkaEventOutboxRepository = kafkaEventOutboxRepository;

        inboundUniqueCounter = counter(meterRegistry, "idempotency.inbound.total", "result", "unique");
        inboundDuplicateCounter = counter(meterRegistry, "idempotency.inbound.total", "result", "duplicate");
        inboundInvalidCounter = counter(meterRegistry, "idempotency.inbound.total", "result", "invalid");
        asyncReplySuccessCounter = counter(meterRegistry, "idempotency.async_reply.total", "result", "success");
        asyncReplyResendCounter = counter(meterRegistry, "idempotency.async_reply.total", "result", "resend");
        asyncReplyFailureCounter = counter(meterRegistry, "idempotency.async_reply.total", "result", "failure");
        asyncReplyTimeoutCounter = counter(meterRegistry, "idempotency.async_reply.total", "result", "timeout");
        asyncReplyOrphanCounter = counter(meterRegistry, "idempotency.async_reply.total", "result", "orphan");
        asyncReplyInvalidCounter = counter(meterRegistry, "idempotency.async_reply.total", "result", "invalid");
        deliverySuccessCounter = counter(meterRegistry, "idempotency.delivery.total", "result", "success");
        deliveryRetryCounter = counter(meterRegistry, "idempotency.delivery.total", "result", "retry");
        deliveryFailureCounter = counter(meterRegistry, "idempotency.delivery.total", "result", "failure");
        outboxSuccessCounter = counter(meterRegistry, "idempotency.outbox.total", "result", "success");
        outboxRetryCounter = counter(meterRegistry, "idempotency.outbox.total", "result", "retry");
        outboxFailureCounter = counter(meterRegistry, "idempotency.outbox.total", "result", "failure");
        deliveryOwnershipLostCounter = counter(meterRegistry, "idempotency.worker.ownership_lost.total", "worker", "delivery");
        outboxOwnershipLostCounter = counter(meterRegistry, "idempotency.worker.ownership_lost.total", "worker", "outbox");
        deliveryDurationTimer = Timer.builder("idempotency.delivery.duration").publishPercentileHistogram().register(meterRegistry);
        outboxDurationTimer = Timer.builder("idempotency.outbox.duration").publishPercentileHistogram().register(meterRegistry);

        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.RESERVED);
        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.COMMITTED);
        registerIdempotencyBacklogGauge(meterRegistry, IdempotencyStatus.ERROR);
        registerOutboxBacklogGauge(meterRegistry, OutboxStatus.NEW);
        registerOutboxBacklogGauge(meterRegistry, OutboxStatus.DONE);
        registerOutboxBacklogGauge(meterRegistry, OutboxStatus.ERROR);
        Gauge.builder("idempotency.leases.expired", this, CoreMetrics::countExpiredIdempotencyLeases).register(meterRegistry);
        Gauge.builder("idempotency.outbox.leases.expired", this, CoreMetrics::countExpiredOutboxLeases).register(meterRegistry);
    }

    public void recordInboundUnique() {
        inboundUniqueCounter.increment();
    }

    public void recordInboundDuplicate() {
        inboundDuplicateCounter.increment();
    }

    public void recordInboundInvalid() {
        inboundInvalidCounter.increment();
    }

    public void recordAsyncReplySuccess() {
        asyncReplySuccessCounter.increment();
    }

    public void recordAsyncReplyResend() {
        asyncReplyResendCounter.increment();
    }

    public void recordAsyncReplyFailure() {
        asyncReplyFailureCounter.increment();
    }

    public void recordAsyncReplyTimeout() {
        asyncReplyTimeoutCounter.increment();
    }

    public void recordAsyncReplyOrphan() {
        asyncReplyOrphanCounter.increment();
    }

    public void recordAsyncReplyInvalid() {
        asyncReplyInvalidCounter.increment();
    }

    public void recordDeliverySuccess(Duration duration) {
        deliverySuccessCounter.increment();
        deliveryDurationTimer.record(duration);
    }

    public void recordDeliveryRetry(Duration duration) {
        deliveryRetryCounter.increment();
        deliveryDurationTimer.record(duration);
    }

    public void recordDeliveryFailure(Duration duration) {
        deliveryFailureCounter.increment();
        deliveryDurationTimer.record(duration);
    }

    public void recordOutboxSuccess(Duration duration) {
        outboxSuccessCounter.increment();
        outboxDurationTimer.record(duration);
    }

    public void recordOutboxRetry(Duration duration) {
        outboxRetryCounter.increment();
        outboxDurationTimer.record(duration);
    }

    public void recordOutboxFailure(Duration duration) {
        outboxFailureCounter.increment();
        outboxDurationTimer.record(duration);
    }

    public void recordDeliveryOwnershipLost() {
        deliveryOwnershipLostCounter.increment();
    }

    public void recordOutboxOwnershipLost() {
        outboxOwnershipLostCounter.increment();
    }

    private Counter counter(MeterRegistry meterRegistry, String name, String tagKey, String tagValue) {
        return Counter.builder(name).tag(tagKey, tagValue).register(meterRegistry);
    }

    private void registerIdempotencyBacklogGauge(MeterRegistry meterRegistry, IdempotencyStatus status) {
        Gauge.builder("idempotency.queue.backlog", idempotencyRepository, repository -> repository.countByStatus(status))
                .tag("status", status.name())
                .register(meterRegistry);
    }

    private void registerOutboxBacklogGauge(MeterRegistry meterRegistry, OutboxStatus status) {
        Gauge.builder("idempotency.outbox.backlog", kafkaEventOutboxRepository, repository -> repository.countByStatus(status))
                .tag("status", status.name())
                .register(meterRegistry);
    }

    private double countExpiredIdempotencyLeases() {
        return idempotencyRepository.countExpiredLeases(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private double countExpiredOutboxLeases() {
        return kafkaEventOutboxRepository.countExpiredLeases(OffsetDateTime.now(ZoneOffset.UTC));
    }
}

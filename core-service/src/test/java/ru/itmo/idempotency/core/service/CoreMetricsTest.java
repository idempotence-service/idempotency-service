package ru.itmo.idempotency.core.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreMetricsTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;
    @Mock
    private StorageShardExecutor storageShardExecutor;

    private MeterRegistry meterRegistry;
    private CoreMetrics coreMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        when(storageShardExecutor.sumLongReadOnly(any())).thenReturn(3L);
        coreMetrics = new CoreMetrics(meterRegistry, idempotencyRepository, kafkaEventOutboxRepository, storageShardExecutor);
    }

    @Test
    void shouldIncrementAllInboundCounters() {
        coreMetrics.recordInboundUnique();
        coreMetrics.recordInboundDuplicate();
        coreMetrics.recordInboundInvalid();

        assertCounter("idempotency.inbound.total", "result", "unique", 1);
        assertCounter("idempotency.inbound.total", "result", "duplicate", 1);
        assertCounter("idempotency.inbound.total", "result", "invalid", 1);
    }

    @Test
    void shouldIncrementAllAsyncReplyCounters() {
        coreMetrics.recordAsyncReplySuccess();
        coreMetrics.recordAsyncReplyResend();
        coreMetrics.recordAsyncReplyFailure();
        coreMetrics.recordAsyncReplyTimeout();
        coreMetrics.recordAsyncReplyOrphan();
        coreMetrics.recordAsyncReplyInvalid();

        assertCounter("idempotency.async_reply.total", "result", "success", 1);
        assertCounter("idempotency.async_reply.total", "result", "resend", 1);
        assertCounter("idempotency.async_reply.total", "result", "failure", 1);
        assertCounter("idempotency.async_reply.total", "result", "timeout", 1);
        assertCounter("idempotency.async_reply.total", "result", "orphan", 1);
        assertCounter("idempotency.async_reply.total", "result", "invalid", 1);
    }

    @Test
    void shouldIncrementDeliveryAndOutboxCounters() {
        Duration duration = Duration.ofMillis(50);
        coreMetrics.recordDeliverySuccess(duration);
        coreMetrics.recordDeliveryRetry(duration);
        coreMetrics.recordDeliveryFailure(duration);
        coreMetrics.recordOutboxSuccess(duration);
        coreMetrics.recordOutboxRetry(duration);
        coreMetrics.recordOutboxFailure(duration);
        coreMetrics.recordDeliveryOwnershipLost();
        coreMetrics.recordOutboxOwnershipLost();

        assertCounter("idempotency.delivery.total", "result", "success", 1);
        assertCounter("idempotency.delivery.total", "result", "retry", 1);
        assertCounter("idempotency.delivery.total", "result", "failure", 1);
        assertCounter("idempotency.outbox.total", "result", "success", 1);
        assertCounter("idempotency.outbox.total", "result", "retry", 1);
        assertCounter("idempotency.outbox.total", "result", "failure", 1);
        assertCounter("idempotency.worker.ownership_lost.total", "worker", "delivery", 1);
        assertCounter("idempotency.worker.ownership_lost.total", "worker", "outbox", 1);
        assertTrue(meterRegistry.get("idempotency.delivery.duration").timer().count() >= 3);
        assertTrue(meterRegistry.get("idempotency.outbox.duration").timer().count() >= 3);
    }

    @Test
    void shouldExposeBacklogAndLeaseGauges() {
        when(idempotencyRepository.countByStatus(any())).thenReturn(2L);
        when(idempotencyRepository.countExpiredLeases(any(OffsetDateTime.class))).thenReturn(1L);
        when(kafkaEventOutboxRepository.countByStatus(any())).thenReturn(4L);
        when(kafkaEventOutboxRepository.countExpiredLeases(any(OffsetDateTime.class))).thenReturn(2L);

        for (IdempotencyStatus status : IdempotencyStatus.values()) {
            assertEquals(3.0, meterRegistry.get("idempotency.queue.backlog").tag("status", status.name()).gauge().value());
        }
        for (OutboxStatus status : OutboxStatus.values()) {
            assertEquals(3.0, meterRegistry.get("idempotency.outbox.backlog").tag("status", status.name()).gauge().value());
        }
        assertEquals(3.0, meterRegistry.get("idempotency.leases.expired").gauge().value());
        assertEquals(3.0, meterRegistry.get("idempotency.outbox.leases.expired").gauge().value());
    }

    private void assertCounter(String name, String tagKey, String tagValue, double expected) {
        assertEquals(expected, meterRegistry.get(name).tag(tagKey, tagValue).counter().count());
    }
}

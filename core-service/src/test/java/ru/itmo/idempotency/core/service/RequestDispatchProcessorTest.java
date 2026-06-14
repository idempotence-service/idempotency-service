package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.kafka.common.errors.NetworkException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.ProcessingResult;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestDispatchProcessorTest {

    @Mock
    private KafkaEventOutboxService kafkaEventOutboxService;
    @Mock
    private CoreJsonSupport coreJsonSupport;
    @Mock
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;
    @Mock
    private CoreMetrics coreMetrics;
    @Mock
    private MdcContextSupport mdcContextSupport;
    @Mock
    private StorageShardExecutor storageShardExecutor;

    private CoreProperties coreProperties;

    @InjectMocks
    private RequestDispatchProcessor processor;

    @BeforeEach
    void setUp() {
        coreProperties = new CoreProperties();
        coreProperties.setInstanceId("worker-1");
        processor = new RequestDispatchProcessor(
                kafkaEventOutboxService,
                coreJsonSupport,
                kafkaJsonProducerRegistry,
                coreProperties,
                coreMetrics,
                mdcContextSupport,
                storageShardExecutor
        );
        when(mdcContextSupport.open(any(), any(), any(), any())).thenReturn(() -> {});
    }

    @Test
    void processBatchReturnsZeroWhenNoClaims() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.empty());

        assertEquals(0, processor.processBatch(10));
    }

    @Test
    void processBatchStopsAtBatchLimit() {
        KafkaEventOutboxEntity entity = outboxEntity();
        AtomicInteger claims = new AtomicInteger();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenAnswer(invocation -> {
            if (claims.incrementAndGet() <= 2) {
                return Optional.of(entity);
            }
            return Optional.empty();
        });
        stubSnapshotWithRequestOut();
        when(kafkaEventOutboxService.completeClaimedDispatch(anyString(), anyLong(), anyString(), isNull())).thenReturn(true);

        assertEquals(2, processor.processBatch(2));
    }

    @Test
    void failsWhenRequestOutMissing() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotWithoutRequestOut());
        when(kafkaEventOutboxService.failClaimedDispatch(anyString(), anyLong(), anyString(), anyString())).thenReturn(true);

        assertEquals(1, processor.processBatch(1));

        verify(kafkaJsonProducerRegistry, never()).send(anyString(), anyString(), anyString(), any());
        verify(coreMetrics).recordOutboxFailure(any(Duration.class));
    }

    @Test
    void recordsOwnershipLostWhenFailClaimedDispatchReturnsFalse() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotWithoutRequestOut());
        when(kafkaEventOutboxService.failClaimedDispatch(anyString(), anyLong(), anyString(), anyString())).thenReturn(false);

        processor.processBatch(1);

        verify(coreMetrics).recordOutboxOwnershipLost();
    }

    @Test
    void completesSuccessfulDispatch() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        stubSnapshotWithRequestOut();
        when(kafkaEventOutboxService.completeClaimedDispatch(eq("gk-1"), eq(1L), eq("worker-1"), isNull())).thenReturn(true);

        processor.processBatch(1);

        verify(kafkaJsonProducerRegistry).send(eq("localhost:9092"), eq("request-out"), eq("gk-1"), any());
        verify(coreMetrics).recordOutboxSuccess(any(Duration.class));
    }

    @Test
    void recordsOwnershipLostOnCompleteFailure() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        stubSnapshotWithRequestOut();
        when(kafkaEventOutboxService.completeClaimedDispatch(anyString(), anyLong(), anyString(), isNull())).thenReturn(false);

        processor.processBatch(1);

        verify(coreMetrics).recordOutboxOwnershipLost();
    }

    @Test
    void retriesOnRetriableKafkaError() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        stubSnapshotWithRequestOut();
        doThrow(new IllegalStateException("send failed", new NetworkException("broker down")))
                .when(kafkaJsonProducerRegistry).send(anyString(), anyString(), anyString(), any());
        when(kafkaEventOutboxService.retryClaimedDispatch(anyString(), anyLong(), anyString(), anyString(), any(Duration.class), eq(5)))
                .thenReturn(true);

        processor.processBatch(1);

        verify(coreMetrics).recordOutboxRetry(any(Duration.class));
    }

    @Test
    void recordsOwnershipLostOnRetryFailure() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        stubSnapshotWithRequestOut();
        doThrow(new IllegalStateException("send failed", new NetworkException("broker down")))
                .when(kafkaJsonProducerRegistry).send(anyString(), anyString(), anyString(), any());
        when(kafkaEventOutboxService.retryClaimedDispatch(anyString(), anyLong(), anyString(), anyString(), any(Duration.class), anyInt()))
                .thenReturn(false);

        processor.processBatch(1);

        verify(coreMetrics).recordOutboxOwnershipLost();
    }

    @Test
    void failsOnPermanentKafkaError() {
        KafkaEventOutboxEntity entity = outboxEntity();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity));
        stubSnapshotWithRequestOut();
        doThrow(new IllegalStateException("permanent"))
                .when(kafkaJsonProducerRegistry).send(anyString(), anyString(), anyString(), any());
        when(kafkaEventOutboxService.failClaimedDispatch(anyString(), anyLong(), anyString(), anyString())).thenReturn(true);

        processor.processBatch(1);

        verify(coreMetrics).recordOutboxFailure(any(Duration.class));
    }

    private void stubSnapshotWithRequestOut() {
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotWithRequestOut());
        when(coreJsonSupport.toJsonNode(any())).thenReturn(JsonNodeFactory.instance.objectNode());
    }

    private static KafkaEventOutboxEntity outboxEntity() {
        return KafkaEventOutboxEntity.builder()
                .id(1L)
                .globalKey("gk-1")
                .yamlSnapshot(new ObjectMapper().createObjectNode())
                .result(ProcessingResult.SUCCESS)
                .resultDescription("ok")
                .build();
    }

    private static RouteModels.RouteSnapshot snapshotWithRequestOut() {
        RouteModels.RouteChannel requestOut = new RouteModels.RouteChannel("localhost:9092", "request-out", "g", 1, (short) 1);
        return new RouteModels.RouteSnapshot("svc", "int", null, requestOut, null, null, true);
    }

    private static RouteModels.RouteSnapshot snapshotWithoutRequestOut() {
        return new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);
    }
}

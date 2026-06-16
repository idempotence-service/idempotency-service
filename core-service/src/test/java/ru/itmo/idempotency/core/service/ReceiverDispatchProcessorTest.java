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
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
class ReceiverDispatchProcessorTest {

    @Mock
    private IdempotencyService idempotencyService;
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
    private ReceiverDispatchProcessor processor;

    @BeforeEach
    void setUp() {
        coreProperties = new CoreProperties();
        coreProperties.setInstanceId("worker-1");
        processor = new ReceiverDispatchProcessor(
                idempotencyService,
                coreJsonSupport,
                kafkaJsonProducerRegistry,
                coreProperties,
                coreMetrics,
                mdcContextSupport,
                storageShardExecutor
        );
        when(mdcContextSupport.open(any(), any(), any(), any())).thenReturn(() -> {});
        when(coreJsonSupport.toMap(any())).thenReturn(Map.of("globalKey", "gk-1", "uid", "uid-1"));
    }

    @Test
    void processBatchReturnsZeroWhenNoClaims() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.empty());

        assertEquals(0, processor.processBatch(10));
    }

    @Test
    void processBatchHonorsBatchLimit() {
        IdempotencyEntity entity = entity();
        AtomicInteger claims = new AtomicInteger();
        when(storageShardExecutor.firstPresentInTransaction(any())).thenAnswer(invocation -> {
            if (claims.incrementAndGet() <= 3) {
                return Optional.of(entity);
            }
            return Optional.empty();
        });
        stubSyncSnapshot();
        when(idempotencyService.completeClaimedDelivery(anyString(), anyString(), eq(IdempotencyStatus.COMMITTED), isNull()))
                .thenReturn(true);

        assertEquals(3, processor.processBatch(3));
    }

    @Test
    void failsWhenReplyInMissing() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotWithoutReplyIn());
        when(idempotencyService.failClaimedDelivery(anyString(), anyString(), anyString())).thenReturn(true);

        processor.processBatch(1);

        verify(kafkaJsonProducerRegistry, never()).send(anyString(), anyString(), anyString(), any());
        verify(coreMetrics).recordDeliveryFailure(eq("int"), any(Duration.class));
    }

    @Test
    void recordsOwnershipLostOnReplyInSchemaFailure() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotWithoutReplyIn());
        when(idempotencyService.failClaimedDelivery(anyString(), anyString(), anyString())).thenReturn(false);

        processor.processBatch(1);

        verify(coreMetrics).recordDeliveryOwnershipLost("int");
    }

    @Test
    void completesSyncDeliveryAsCommitted() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        stubSyncSnapshot();
        when(idempotencyService.completeClaimedDelivery("gk-1", "worker-1", IdempotencyStatus.COMMITTED, null))
                .thenReturn(true);

        processor.processBatch(1);

        verify(kafkaJsonProducerRegistry).send(eq("localhost:9092"), eq("reply-in"), eq("gk-1"), any());
        verify(coreMetrics).recordDeliverySuccess(eq("int"), any(Duration.class));
    }

    @Test
    void completesAsyncDeliveryAsWaiting() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotWithAsyncReply());
        when(idempotencyService.completeClaimedDelivery("gk-1", "worker-1", IdempotencyStatus.WAITING_ASYNC_RESPONSE, null))
                .thenReturn(true);

        processor.processBatch(1);

        verify(coreMetrics).recordDeliverySuccess(eq("int"), any(Duration.class));
    }

    @Test
    void recordsOwnershipLostOnCompleteFailure() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        stubSyncSnapshot();
        when(idempotencyService.completeClaimedDelivery(anyString(), anyString(), any(), isNull())).thenReturn(false);

        processor.processBatch(1);

        verify(coreMetrics).recordDeliveryOwnershipLost("int");
    }

    @Test
    void retriesOnRetriableError() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        stubSyncSnapshot();
        doThrow(new IllegalStateException("send failed", new NetworkException("down")))
                .when(kafkaJsonProducerRegistry).send(anyString(), anyString(), anyString(), any());
        when(idempotencyService.retryClaimedDelivery(anyString(), anyString(), anyString(), any(Duration.class), eq(5)))
                .thenReturn(true);

        processor.processBatch(1);

        verify(coreMetrics).recordDeliveryRetry(eq("int"), any(Duration.class));
    }

    @Test
    void recordsOwnershipLostOnRetryFailure() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        stubSyncSnapshot();
        doThrow(new IllegalStateException("send failed", new NetworkException("down")))
                .when(kafkaJsonProducerRegistry).send(anyString(), anyString(), anyString(), any());
        when(idempotencyService.retryClaimedDelivery(anyString(), anyString(), anyString(), any(Duration.class), anyInt()))
                .thenReturn(false);

        processor.processBatch(1);

        verify(coreMetrics).recordDeliveryOwnershipLost("int");
    }

    @Test
    void failsOnPermanentError() {
        when(storageShardExecutor.firstPresentInTransaction(any())).thenReturn(Optional.of(entity()));
        stubSyncSnapshot();
        doThrow(new IllegalStateException("permanent"))
                .when(kafkaJsonProducerRegistry).send(anyString(), anyString(), anyString(), any());
        when(idempotencyService.failClaimedDelivery(anyString(), anyString(), anyString())).thenReturn(true);

        processor.processBatch(1);

        verify(coreMetrics).recordDeliveryFailure(eq("int"), any(Duration.class));
    }

    private void stubSyncSnapshot() {
        when(coreJsonSupport.parseSnapshot(any())).thenReturn(snapshotSync());
    }

    private static IdempotencyEntity entity() {
        var headers = JsonNodeFactory.instance.objectNode();
        headers.put("traceId", "t-1");
        return IdempotencyEntity.builder()
                .globalKey("gk-1")
                .sourceUid("uid-1")
                .serviceName("svc")
                .integrationName("int")
                .yamlSnapshot(new ObjectMapper().createObjectNode())
                .headers(headers)
                .payload(JsonNodeFactory.instance.objectNode())
                .status(IdempotencyStatus.RESERVED)
                .build();
    }

    private static RouteModels.RouteSnapshot snapshotSync() {
        RouteModels.RouteChannel replyIn = new RouteModels.RouteChannel("localhost:9092", "reply-in", "g", 1, (short) 1);
        return new RouteModels.RouteSnapshot("svc", "int", null, null, replyIn, null, true);
    }

    private static RouteModels.RouteSnapshot snapshotWithAsyncReply() {
        RouteModels.RouteChannel replyIn = new RouteModels.RouteChannel("localhost:9092", "reply-in", "g", 1, (short) 1);
        RouteModels.RouteChannel replyOut = new RouteModels.RouteChannel("localhost:9092", "reply-out", "g", 1, (short) 1);
        return new RouteModels.RouteSnapshot("svc", "int", null, null, replyIn, replyOut, true);
    }

    private static RouteModels.RouteSnapshot snapshotWithoutReplyIn() {
        return new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);
    }
}

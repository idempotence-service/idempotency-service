package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.domain.ProcessingResult;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KafkaEventOutboxServiceTest {

    @Mock
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private StorageShardExecutor storageShardExecutor;

    @InjectMocks
    private KafkaEventOutboxService kafkaEventOutboxService;

    @BeforeEach
    void setUp() {
        when(storageShardExecutor.runOnKey(anyString(), org.mockito.ArgumentMatchers.<Supplier<?>>any()))
                .thenAnswer(invocation -> {
                    Supplier<?> action = invocation.getArgument(1);
                    return action.get();
                });
    }

    @Test
    void shouldSaveNewOutboxEntity() {
        RouteModels.RouteSnapshot snapshot = new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);
        when(kafkaEventOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        KafkaEventOutboxEntity saved = kafkaEventOutboxService.save("gk-1", snapshot, ProcessingResult.SUCCESS, "ok");

        assertEquals(OutboxStatus.NEW, saved.getStatus());
        assertEquals("gk-1", saved.getGlobalKey());
    }

    @Test
    void shouldClaimNextNewEntity() {
        KafkaEventOutboxEntity entity = outbox(0);
        when(kafkaEventOutboxRepository.lockFirstAvailableByStatus(OutboxStatus.NEW.name())).thenReturn(Optional.of(entity));
        when(kafkaEventOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<KafkaEventOutboxEntity> claimed = kafkaEventOutboxService.claimNextNew("worker-1", Duration.ofSeconds(30));

        assertTrue(claimed.isPresent());
        assertEquals("worker-1", claimed.get().getOwnerId());
    }

    @Test
    void shouldMarkErrorWhenScheduleRetryReachesMaxAttempts() {
        KafkaEventOutboxEntity entity = outbox(4);
        when(kafkaEventOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        KafkaEventOutboxEntity updated = kafkaEventOutboxService.scheduleRetry(entity, "fail", Duration.ofSeconds(5), 5);

        assertEquals(OutboxStatus.ERROR, updated.getStatus());
        assertEquals(5, updated.getRetryCount());
    }

    @Test
    void shouldReturnNewStatusWhenScheduleRetryBelowMaxAttempts() {
        KafkaEventOutboxEntity entity = outbox(1);
        when(kafkaEventOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        KafkaEventOutboxEntity updated = kafkaEventOutboxService.scheduleRetry(entity, "retry", Duration.ofSeconds(5), 5);

        assertEquals(OutboxStatus.NEW, updated.getStatus());
        assertEquals(2, updated.getRetryCount());
    }

    @Test
    void shouldReturnFalseWhenCompleteClaimedDispatchOwnerMismatch() {
        KafkaEventOutboxEntity entity = outbox(0);
        entity.setOwnerId("other");
        when(kafkaEventOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));

        assertFalse(kafkaEventOutboxService.completeClaimedDispatch("gk-1", 1L, "worker-1", null));
    }

    @Test
    void shouldReturnFalseWhenRetryClaimedDispatchOwnerMismatch() {
        when(kafkaEventOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertFalse(kafkaEventOutboxService.retryClaimedDispatch("gk-1", 1L, "worker-1", "x", Duration.ofSeconds(1), 5));
    }

    @Test
    void shouldReturnFalseWhenFailClaimedDispatchOwnerMismatch() {
        KafkaEventOutboxEntity entity = outbox(0);
        entity.setOwnerId("worker-2");
        when(kafkaEventOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));

        assertFalse(kafkaEventOutboxService.failClaimedDispatch("gk-1", 1L, "worker-1", "error"));
    }

    @Test
    void shouldCompleteClaimedDispatchWhenOwnerMatches() {
        KafkaEventOutboxEntity entity = outbox(0);
        entity.setOwnerId("worker-1");
        when(kafkaEventOutboxRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entity));
        when(kafkaEventOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(kafkaEventOutboxService.completeClaimedDispatch("gk-1", 1L, "worker-1", null));
        assertEquals(OutboxStatus.DONE, entity.getStatus());
    }

    @Test
    void shouldSkipDeleteForNullOrEmptyCollection() {
        kafkaEventOutboxService.delete(null);
        kafkaEventOutboxService.delete(List.of());

        verify(storageShardExecutor, never()).runOnKey(anyString(), org.mockito.ArgumentMatchers.<Supplier<?>>any());
    }

    @Test
    void shouldChangeStatusAndMarkAsError() {
        KafkaEventOutboxEntity entity = outbox(0);
        when(kafkaEventOutboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        KafkaEventOutboxEntity error = kafkaEventOutboxService.markAsError(entity, "boom");

        assertEquals(OutboxStatus.ERROR, error.getStatus());
    }

    private static KafkaEventOutboxEntity outbox(int retryCount) {
        return KafkaEventOutboxEntity.builder()
                .id(1L)
                .globalKey("gk-1")
                .yamlSnapshot(JsonNodeFactory.instance.objectNode())
                .status(OutboxStatus.NEW)
                .result(ProcessingResult.SUCCESS)
                .retryCount(retryCount)
                .nextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}

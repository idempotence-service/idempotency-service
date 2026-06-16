package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private StorageShardExecutor storageShardExecutor;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        when(storageShardExecutor.runOnKey(anyString(), org.mockito.ArgumentMatchers.<Supplier<?>>any()))
                .thenAnswer(invocation -> {
                    Supplier<?> action = invocation.getArgument(1);
                    return action.get();
                });
    }

    @Test
    void shouldMarkErrorWhenScheduleRetryReachesMaxAttempts() {
        IdempotencyEntity entity = entity(4);
        when(idempotencyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyEntity updated = idempotencyService.scheduleRetry(entity, "fail", Duration.ofSeconds(5), 5);

        assertEquals(IdempotencyStatus.ERROR, updated.getStatus());
        assertEquals(5, updated.getRetryCount());
        assertTrue(updated.getStatusDescription().contains("Достигнут лимит"));
    }

    @Test
    void shouldReserveWhenScheduleRetryBelowMaxAttempts() {
        IdempotencyEntity entity = entity(1);
        when(idempotencyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyEntity updated = idempotencyService.scheduleRetry(entity, "retry", Duration.ofSeconds(5), 5);

        assertEquals(IdempotencyStatus.RESERVED, updated.getStatus());
        assertEquals(2, updated.getRetryCount());
    }

    @Test
    void shouldReturnFalseWhenCompleteClaimedDeliveryOwnerMismatch() {
        IdempotencyEntity entity = entity(0);
        entity.setOwnerId("other");
        when(idempotencyRepository.findByGlobalKeyForUpdate("gk-1")).thenReturn(Optional.of(entity));

        assertFalse(idempotencyService.completeClaimedDelivery("gk-1", "worker-1", IdempotencyStatus.COMMITTED, null));
    }

    @Test
    void shouldReturnFalseWhenRetryClaimedDeliveryOwnerMismatch() {
        when(idempotencyRepository.findByGlobalKeyForUpdate("gk-1")).thenReturn(Optional.empty());

        assertFalse(idempotencyService.retryClaimedDelivery("gk-1", "worker-1", "x", Duration.ofSeconds(1), 5));
    }

    @Test
    void shouldReturnFalseWhenFailClaimedDeliveryOwnerMismatch() {
        IdempotencyEntity entity = entity(0);
        entity.setOwnerId("worker-2");
        when(idempotencyRepository.findByGlobalKeyForUpdate("gk-1")).thenReturn(Optional.of(entity));

        assertFalse(idempotencyService.failClaimedDelivery("gk-1", "worker-1", "error"));
    }

    @Test
    void shouldSkipDeleteForNullOrEmptyCollection() {
        idempotencyService.delete(null);
        idempotencyService.delete(List.of());

        verify(storageShardExecutor, never()).runOnKey(anyString(), org.mockito.ArgumentMatchers.<Supplier<?>>any());
    }

    @Test
    void shouldRestartEntity() {
        IdempotencyEntity entity = entity(0);
        entity.setStatus(IdempotencyStatus.ERROR);
        entity.setRetryCount(3);
        entity.setStatusDescription("failed");
        when(idempotencyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyEntity restarted = idempotencyService.restart(entity);

        assertEquals(IdempotencyStatus.RESERVED, restarted.getStatus());
        assertEquals(0, restarted.getRetryCount());
        assertEquals(null, restarted.getStatusDescription());
    }

    @Test
    void shouldCompleteClaimedDeliveryWhenOwnerMatches() {
        IdempotencyEntity entity = entity(0);
        entity.setOwnerId("worker-1");
        when(idempotencyRepository.findByGlobalKeyForUpdate("gk-1")).thenReturn(Optional.of(entity));
        when(idempotencyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(idempotencyService.completeClaimedDelivery("gk-1", "worker-1", IdempotencyStatus.COMMITTED, "done"));
        assertEquals(IdempotencyStatus.COMMITTED, entity.getStatus());
    }

    @Test
    void shouldSaveNewEntity() {
        RouteModels.RouteSnapshot snapshot = new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);
        when(idempotencyRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdempotencyEntity saved = idempotencyService.save(
                "gk-1", "uid-1", snapshot, JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode()
        );

        assertEquals("gk-1", saved.getGlobalKey());
        assertEquals(IdempotencyStatus.RESERVED, saved.getStatus());
    }

    private static IdempotencyEntity entity(int retryCount) {
        return IdempotencyEntity.builder()
                .globalKey("gk-1")
                .sourceUid("uid-1")
                .serviceName("svc")
                .integrationName("int")
                .yamlSnapshot(JsonNodeFactory.instance.objectNode())
                .headers(JsonNodeFactory.instance.objectNode())
                .payload(JsonNodeFactory.instance.objectNode())
                .status(IdempotencyStatus.RESERVED)
                .retryCount(retryCount)
                .nextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}

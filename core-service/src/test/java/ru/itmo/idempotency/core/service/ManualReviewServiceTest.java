package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.EventAuditRepository;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualReviewServiceTest {

    @Mock
    private IdempotencyRepository idempotencyRepository;
    @Mock
    private EventAuditRepository eventAuditRepository;
    @Mock
    private IdempotencySearchService idempotencySearchService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private StorageShardExecutor storageShardExecutor;

    @InjectMocks
    private ManualReviewService manualReviewService;

    @Test
    void shouldThrowWhenRestartTargetNotFound() {
        when(idempotencySearchService.acquireUniqueWaitIfLocked("missing-key")).thenReturn(Optional.empty());

        Assertions.assertThrows(EventNotFoundException.class, () -> manualReviewService.restart("missing-key"));
        verify(idempotencyService, never()).restart(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnAlreadyRestartedMessageWhenStatusIsNotError() {
        IdempotencyEntity entity = IdempotencyEntity.builder()
                .globalKey("gk-1")
                .status(IdempotencyStatus.RESERVED)
                .build();
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));

        String message = manualReviewService.restart("gk-1");

        Assertions.assertEquals("Задача уже перезапущена", message);
        verify(idempotencyService, never()).restart(entity);
    }

    @Test
    void shouldRestartErrorEvent() {
        IdempotencyEntity entity = IdempotencyEntity.builder()
                .globalKey("gk-1")
                .status(IdempotencyStatus.ERROR)
                .yamlSnapshot(JsonNodeFactory.instance.objectNode())
                .headers(JsonNodeFactory.instance.objectNode())
                .payload(JsonNodeFactory.instance.objectNode())
                .build();
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));

        String message = manualReviewService.restart("gk-1");

        Assertions.assertEquals("Задача успешно перезапущена", message);
        verify(idempotencyService).restart(entity);
    }
}

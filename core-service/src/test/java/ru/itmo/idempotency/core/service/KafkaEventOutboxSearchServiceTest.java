package ru.itmo.idempotency.core.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventOutboxSearchServiceTest {

    @Mock
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;

    @InjectMocks
    private KafkaEventOutboxSearchService kafkaEventOutboxSearchService;

    @Test
    void shouldDelegateToLockFirstAvailableByStatus() {
        KafkaEventOutboxEntity entity = KafkaEventOutboxEntity.builder()
                .id(1L)
                .status(OutboxStatus.NEW)
                .build();
        when(kafkaEventOutboxRepository.lockFirstAvailableByStatus(OutboxStatus.NEW.name()))
                .thenReturn(Optional.of(entity));

        Optional<KafkaEventOutboxEntity> result = kafkaEventOutboxSearchService.acquireFirstNotLocked();

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1L, result.get().getId());
        verify(kafkaEventOutboxRepository).lockFirstAvailableByStatus(OutboxStatus.NEW.name());
    }
}

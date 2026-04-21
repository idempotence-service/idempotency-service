package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.domain.ProcessingResult;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class KafkaEventOutboxService {

    private final KafkaEventOutboxRepository kafkaEventOutboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public KafkaEventOutboxEntity save(String globalKey,
                                       RouteModels.RouteSnapshot snapshot,
                                       ProcessingResult result,
                                       String resultDescription) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return kafkaEventOutboxRepository.save(KafkaEventOutboxEntity.builder()
                .globalKey(globalKey)
                .serviceName(snapshot.service())
                .integrationName(snapshot.integration())
                .yamlSnapshot(objectMapper.valueToTree(snapshot))
                .status(OutboxStatus.NEW)
                .result(result)
                .resultDescription(DescriptionUtils.limit(resultDescription))
                .retryCount(0)
                .nextAttemptDate(now)
                .build());
    }

    @Transactional
    public KafkaEventOutboxEntity changeStatus(KafkaEventOutboxEntity entity, OutboxStatus status, String description) {
        entity.setStatus(status);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        return kafkaEventOutboxRepository.save(entity);
    }

    @Transactional
    public KafkaEventOutboxEntity markAsError(KafkaEventOutboxEntity entity, String description) {
        return changeStatus(entity, OutboxStatus.ERROR, description);
    }

    @Transactional
    public KafkaEventOutboxEntity scheduleRetry(KafkaEventOutboxEntity entity,
                                                String description,
                                                Duration delay,
                                                int maxAttempts) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int nextRetryCount = entity.getRetryCount() + 1;
        entity.setRetryCount(nextRetryCount);
        if (nextRetryCount >= maxAttempts) {
            entity.setStatus(OutboxStatus.ERROR);
            entity.setStatusDescription(DescriptionUtils.limit(
                    description + " Достигнут лимит повторных попыток: " + nextRetryCount
            ));
            entity.setNextAttemptDate(now);
            return kafkaEventOutboxRepository.save(entity);
        }

        entity.setStatus(OutboxStatus.NEW);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(now.plus(delay));
        return kafkaEventOutboxRepository.save(entity);
    }
}

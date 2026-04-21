package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;

import java.util.Collection;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public IdempotencyEntity save(String globalKey,
                                  String sourceUid,
                                  RouteModels.RouteSnapshot snapshot,
                                  JsonNode headers,
                                  JsonNode payload) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        IdempotencyEntity entity = IdempotencyEntity.builder()
                .globalKey(globalKey)
                .sourceUid(sourceUid)
                .serviceName(snapshot.service())
                .integrationName(snapshot.integration())
                .yamlSnapshot(objectMapper.valueToTree(snapshot))
                .headers(headers)
                .payload(payload)
                .status(IdempotencyStatus.RESERVED)
                .retryCount(0)
                .nextAttemptDate(now)
                .build();
        return idempotencyRepository.saveAndFlush(entity);
    }

    @Transactional
    public IdempotencyEntity changeStatus(IdempotencyEntity entity, IdempotencyStatus status, String description) {
        entity.setStatus(status);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        return idempotencyRepository.save(entity);
    }

    @Transactional
    public IdempotencyEntity markAsError(IdempotencyEntity entity, String description) {
        return changeStatus(entity, IdempotencyStatus.ERROR, description);
    }

    @Transactional
    public IdempotencyEntity scheduleRetry(IdempotencyEntity entity,
                                           String description,
                                           Duration delay,
                                           int maxAttempts) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int nextRetryCount = entity.getRetryCount() + 1;
        entity.setRetryCount(nextRetryCount);
        if (nextRetryCount >= maxAttempts) {
            entity.setStatus(IdempotencyStatus.ERROR);
            entity.setStatusDescription(DescriptionUtils.limit(
                    description + " Достигнут лимит повторных попыток: " + nextRetryCount
            ));
            entity.setNextAttemptDate(now);
            return idempotencyRepository.save(entity);
        }

        entity.setStatus(IdempotencyStatus.RESERVED);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(now.plus(delay));
        return idempotencyRepository.save(entity);
    }

    @Transactional
    public IdempotencyEntity restart(IdempotencyEntity entity) {
        entity.setStatus(IdempotencyStatus.RESERVED);
        entity.setStatusDescription(null);
        entity.setRetryCount(0);
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        return idempotencyRepository.save(entity);
    }

    @Transactional
    public void delete(Collection<IdempotencyEntity> entities) {
        idempotencyRepository.deleteAllInBatch(entities);
    }
}

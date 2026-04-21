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

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;

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
    public Optional<IdempotencyEntity> claimNextReserved(String ownerId, Duration leaseDuration) {
        return idempotencyRepository.lockFirstAvailableByStatus(IdempotencyStatus.RESERVED.name())
                .map(entity -> claim(entity, ownerId, leaseDuration));
    }

    @Transactional
    public IdempotencyEntity changeStatus(IdempotencyEntity entity, IdempotencyStatus status, String description) {
        applyStatus(entity, status, description);
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
        applyRetry(entity, description, delay, maxAttempts);
        return idempotencyRepository.save(entity);
    }

    @Transactional
    public boolean completeClaimedDelivery(String globalKey,
                                           String ownerId,
                                           IdempotencyStatus status,
                                           String description) {
        return updateClaimed(globalKey, ownerId, entity -> applyStatus(entity, status, description));
    }

    @Transactional
    public boolean retryClaimedDelivery(String globalKey,
                                        String ownerId,
                                        String description,
                                        Duration delay,
                                        int maxAttempts) {
        return updateClaimed(globalKey, ownerId, entity -> applyRetry(entity, description, delay, maxAttempts));
    }

    @Transactional
    public boolean failClaimedDelivery(String globalKey, String ownerId, String description) {
        return updateClaimed(globalKey, ownerId, entity -> applyStatus(entity, IdempotencyStatus.ERROR, description));
    }

    @Transactional
    public IdempotencyEntity restart(IdempotencyEntity entity) {
        applyRestart(entity);
        return idempotencyRepository.save(entity);
    }

    @Transactional
    public void delete(Collection<IdempotencyEntity> entities) {
        idempotencyRepository.deleteAllInBatch(entities);
    }

    private IdempotencyEntity claim(IdempotencyEntity entity, String ownerId, Duration leaseDuration) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entity.setOwnerId(ownerId);
        entity.setLeaseUntil(now.plus(leaseDuration));
        entity.setLastClaimDate(now);
        return idempotencyRepository.save(entity);
    }

    private boolean updateClaimed(String globalKey,
                                  String ownerId,
                                  java.util.function.Consumer<IdempotencyEntity> updater) {
        IdempotencyEntity entity = idempotencyRepository.findByGlobalKeyForUpdate(globalKey).orElse(null);
        if (entity == null || entity.getOwnerId() == null || !entity.getOwnerId().equals(ownerId)) {
            return false;
        }

        updater.accept(entity);
        idempotencyRepository.save(entity);
        return true;
    }

    private void applyStatus(IdempotencyEntity entity, IdempotencyStatus status, String description) {
        entity.setStatus(status);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        clearLease(entity);
    }

    private void applyRetry(IdempotencyEntity entity,
                            String description,
                            Duration delay,
                            int maxAttempts) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int nextRetryCount = entity.getRetryCount() + 1;
        entity.setRetryCount(nextRetryCount);
        if (nextRetryCount >= maxAttempts) {
            entity.setStatus(IdempotencyStatus.ERROR);
            entity.setStatusDescription(DescriptionUtils.limit(limitReachedDescription(description, nextRetryCount)));
            entity.setNextAttemptDate(now);
            clearLease(entity);
            return;
        }

        entity.setStatus(IdempotencyStatus.RESERVED);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(now.plus(delay));
        clearLease(entity);
    }

    private void applyRestart(IdempotencyEntity entity) {
        entity.setStatus(IdempotencyStatus.RESERVED);
        entity.setStatusDescription(null);
        entity.setRetryCount(0);
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        clearLease(entity);
    }

    private void clearLease(IdempotencyEntity entity) {
        entity.setOwnerId(null);
        entity.setLeaseUntil(null);
    }

    private String limitReachedDescription(String description, int attempts) {
        String baseDescription = description == null || description.isBlank() ? "Повторная попытка не удалась." : description;
        return baseDescription + " Достигнут лимит повторных попыток: " + attempts;
    }
}

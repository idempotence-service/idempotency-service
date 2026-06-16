package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Optional;

import javax.sql.DataSource;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public enum AsyncReplyTransitionResult {
        APPLIED,
        ALREADY_RELEASED,
        OWNERSHIP_LOST
    }

    private static final String INSERT_IF_ABSENT_POSTGRES_SQL = """
            INSERT INTO idempotency (
                global_key,
                source_uid,
                service_name,
                integration_name,
                yaml_snapshot,
                headers,
                payload,
                status,
                status_description,
                retry_count,
                next_attempt_date,
                owner_id,
                lease_until,
                last_claim_date,
                create_date,
                update_date
             ) VALUES (
                 :globalKey,
                 :sourceUid,
                 :serviceName,
                 :integrationName,
                CAST(:yamlSnapshot AS JSONB),
                CAST(:headers AS JSONB),
                CAST(:payload AS JSONB),
                :status,
                NULL,
                0,
                :nextAttemptDate,
                NULL,
                NULL,
                 NULL,
                 :createDate,
                 :updateDate
             )
             ON CONFLICT (global_key) DO NOTHING
             """;

    private static final String INSERT_IF_ABSENT_H2_SQL = """
            INSERT INTO idempotency (
                global_key,
                source_uid,
                service_name,
                integration_name,
                yaml_snapshot,
                headers,
                payload,
                status,
                status_description,
                retry_count,
                next_attempt_date,
                owner_id,
                lease_until,
                last_claim_date,
                create_date,
                update_date
            ) VALUES (
                :globalKey,
                :sourceUid,
                :serviceName,
                :integrationName,
                :yamlSnapshot FORMAT JSON,
                :headers FORMAT JSON,
                :payload FORMAT JSON,
                :status,
                NULL,
                0,
                :nextAttemptDate,
                NULL,
                NULL,
                NULL,
                :createDate,
                :updateDate
            )
            """;

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final StorageShardExecutor storageShardExecutor;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final DataSource dataSource;
    @Nullable
    private volatile String cachedDatabaseProductName;

    @Transactional
    public boolean saveIfAbsent(String globalKey,
                                String sourceUid,
                                RouteModels.RouteSnapshot snapshot,
                                JsonNode headers,
                                JsonNode payload) {
        return storageShardExecutor.runOnKey(globalKey, () -> insertIfAbsent(globalKey, sourceUid, snapshot, headers, payload));
    }

    @Transactional
    public IdempotencyEntity save(String globalKey,
                                  String sourceUid,
                                  RouteModels.RouteSnapshot snapshot,
                                  JsonNode headers,
                                  JsonNode payload) {
        return storageShardExecutor.runOnKey(globalKey, () -> {
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
        });
    }

    @Transactional
    public Optional<IdempotencyEntity> claimNextReserved(String ownerId, Duration leaseDuration) {
        return idempotencyRepository.lockFirstAvailableByStatus(IdempotencyStatus.RESERVED.name())
                .map(entity -> claim(entity, ownerId, leaseDuration));
    }

    @Transactional
    public IdempotencyEntity changeStatus(IdempotencyEntity entity, IdempotencyStatus status, String description) {
        return storageShardExecutor.runOnKey(entity.getGlobalKey(), () -> {
            applyStatus(entity, status, description);
            return idempotencyRepository.save(entity);
        });
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
        return storageShardExecutor.runOnKey(entity.getGlobalKey(), () -> {
            applyRetry(entity, description, delay, maxAttempts);
            return idempotencyRepository.save(entity);
        });
    }

    @Transactional
    public boolean completeClaimedDelivery(String globalKey,
                                           String ownerId,
                                           IdempotencyStatus status,
                                           String description) {
        return storageShardExecutor.runOnKey(globalKey,
                () -> updateClaimed(globalKey, ownerId, entity -> applyStatus(entity, status, description)));
    }

    @Transactional
    public AsyncReplyTransitionResult markClaimedWaitingForAsyncReply(String globalKey, String ownerId) {
        return storageShardExecutor.runOnKey(globalKey,
                () -> transitionClaimedToWaitingForAsyncReply(globalKey, ownerId));
    }

    @Transactional
    public AsyncReplyTransitionResult releaseClaimedAsyncReplyWait(String globalKey, String ownerId) {
        return storageShardExecutor.runOnKey(globalKey,
                () -> releaseClaimedAsyncReplyWaitInternal(globalKey, ownerId));
    }

    @Transactional
    public boolean retryClaimedDelivery(String globalKey,
                                        String ownerId,
                                        String description,
                                        Duration delay,
                                        int maxAttempts) {
        return storageShardExecutor.runOnKey(globalKey,
                () -> updateClaimed(globalKey, ownerId, entity -> applyRetry(entity, description, delay, maxAttempts)));
    }

    @Transactional
    public boolean failClaimedDelivery(String globalKey, String ownerId, String description) {
        return storageShardExecutor.runOnKey(globalKey,
                () -> updateClaimed(globalKey, ownerId, entity -> applyStatus(entity, IdempotencyStatus.ERROR, description)));
    }

    @Transactional
    public IdempotencyEntity restart(IdempotencyEntity entity) {
        return storageShardExecutor.runOnKey(entity.getGlobalKey(), () -> {
            applyRestart(entity);
            return idempotencyRepository.save(entity);
        });
    }

    @Transactional
    public void delete(Collection<IdempotencyEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String shardKey = entities.iterator().next().getGlobalKey();
        storageShardExecutor.runOnKey(shardKey, () -> idempotencyRepository.deleteAllInBatch(entities));
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

    private AsyncReplyTransitionResult transitionClaimedToWaitingForAsyncReply(String globalKey, String ownerId) {
        IdempotencyEntity entity = idempotencyRepository.findByGlobalKeyForUpdate(globalKey).orElse(null);
        if (entity == null) {
            return AsyncReplyTransitionResult.OWNERSHIP_LOST;
        }
        if (entity.getOwnerId() != null && entity.getOwnerId().equals(ownerId)) {
            applyWaitingForAsyncReply(entity);
            idempotencyRepository.save(entity);
            return AsyncReplyTransitionResult.APPLIED;
        }
        if (entity.getOwnerId() == null) {
            return AsyncReplyTransitionResult.ALREADY_RELEASED;
        }
        return AsyncReplyTransitionResult.OWNERSHIP_LOST;
    }

    private AsyncReplyTransitionResult releaseClaimedAsyncReplyWaitInternal(String globalKey, String ownerId) {
        IdempotencyEntity entity = idempotencyRepository.findByGlobalKeyForUpdate(globalKey).orElse(null);
        if (entity == null) {
            return AsyncReplyTransitionResult.OWNERSHIP_LOST;
        }
        if (entity.getOwnerId() != null && entity.getOwnerId().equals(ownerId)
                && entity.getStatus() == IdempotencyStatus.WAITING_ASYNC_RESPONSE) {
            clearLease(entity);
            idempotencyRepository.save(entity);
            return AsyncReplyTransitionResult.APPLIED;
        }
        if (entity.getOwnerId() == null) {
            return AsyncReplyTransitionResult.ALREADY_RELEASED;
        }
        return AsyncReplyTransitionResult.OWNERSHIP_LOST;
    }

    private void applyStatus(IdempotencyEntity entity, IdempotencyStatus status, String description) {
        entity.setStatus(status);
        entity.setStatusDescription(DescriptionUtils.limit(description));
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
        clearLease(entity);
    }

    private void applyWaitingForAsyncReply(IdempotencyEntity entity) {
        entity.setStatus(IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        entity.setStatusDescription(null);
        entity.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC));
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

    private boolean insertIfAbsent(String globalKey,
                                   String sourceUid,
                                   RouteModels.RouteSnapshot snapshot,
                                   JsonNode headers,
                                   JsonNode payload) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("globalKey", globalKey)
                .addValue("sourceUid", sourceUid)
                .addValue("serviceName", snapshot.service())
                .addValue("integrationName", snapshot.integration())
                .addValue("yamlSnapshot", toJson(snapshot))
                .addValue("headers", toJson(headers))
                .addValue("payload", toJson(payload))
                .addValue("status", IdempotencyStatus.RESERVED.name())
                .addValue("nextAttemptDate", now)
                .addValue("createDate", now)
                .addValue("updateDate", now);

        try {
            return namedParameterJdbcTemplate.update(insertIfAbsentSql(), parameters) > 0;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    private String limitReachedDescription(String description, int attempts) {
        String baseDescription = description == null || description.isBlank() ? "Повторная попытка не удалась." : description;
        return baseDescription + " Достигнут лимит повторных попыток: " + attempts;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize idempotency payload", exception);
        }
    }

    private String insertIfAbsentSql() {
        String productName = cachedDatabaseProductName;
        if (productName == null) {
            java.sql.Connection connection = DataSourceUtils.getConnection(dataSource);
            try {
                productName = connection.getMetaData().getDatabaseProductName();
                cachedDatabaseProductName = productName;
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("Failed to detect database vendor", exception);
            } finally {
                DataSourceUtils.releaseConnection(connection, dataSource);
            }
        }
        if (productName != null && productName.toLowerCase().contains("h2")) {
            return INSERT_IF_ABSENT_H2_SQL;
        }
        return INSERT_IF_ABSENT_POSTGRES_SQL;
    }
}

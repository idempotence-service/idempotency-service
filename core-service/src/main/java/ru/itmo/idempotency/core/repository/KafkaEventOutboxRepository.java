package ru.itmo.idempotency.core.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;

import java.time.OffsetDateTime;

import java.util.Optional;

public interface KafkaEventOutboxRepository extends JpaRepository<KafkaEventOutboxEntity, Long> {

    @Query(value = """
            SELECT *
            FROM kafka_event_outbox
            WHERE status = :status
              AND next_attempt_date <= CURRENT_TIMESTAMP
              AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP)
            ORDER BY next_attempt_date, create_date
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<KafkaEventOutboxEntity> lockFirstAvailableByStatus(@Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from KafkaEventOutboxEntity entity where entity.id = :id")
    Optional<KafkaEventOutboxEntity> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(OutboxStatus status);

    @Query("select count(entity) from KafkaEventOutboxEntity entity where entity.ownerId is not null and entity.leaseUntil < :threshold")
    long countExpiredLeases(@Param("threshold") OffsetDateTime threshold);
}

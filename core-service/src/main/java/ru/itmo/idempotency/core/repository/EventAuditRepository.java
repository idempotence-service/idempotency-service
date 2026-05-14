package ru.itmo.idempotency.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.idempotency.core.domain.EventAuditEntity;

import java.util.List;

public interface EventAuditRepository extends JpaRepository<EventAuditEntity, Long> {
    List<EventAuditEntity> findByReasonOrderByCreateDateDesc(String reason);
    long countByReason(String reason);

    @Query("SELECT COUNT(DISTINCT e.globalKey) FROM EventAuditEntity e WHERE e.reason = :reason")
    long countDistinctGlobalKeyByReason(@Param("reason") String reason);
}

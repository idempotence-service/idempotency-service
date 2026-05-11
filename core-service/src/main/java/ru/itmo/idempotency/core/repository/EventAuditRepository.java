package ru.itmo.idempotency.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.idempotency.core.domain.EventAuditEntity;

import java.util.List;

public interface EventAuditRepository extends JpaRepository<EventAuditEntity, Long> {
    List<EventAuditEntity> findByReasonOrderByCreateDateDesc(String reason);
}

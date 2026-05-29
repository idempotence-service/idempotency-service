package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.EventAuditEntity;
import ru.itmo.idempotency.core.repository.EventAuditRepository;
import ru.itmo.idempotency.core.storage.StorageShardExecutor;

@Service
@RequiredArgsConstructor
public class EventAuditService {

    private final EventAuditRepository eventAuditRepository;
    private final ObjectMapper objectMapper;
    private final StorageShardExecutor storageShardExecutor;

    @Transactional
    public void save(String globalKey,
                     RouteModels.RouteSnapshot snapshot,
                     String reason,
                     JsonNode headers,
                     JsonNode payload) {
        storageShardExecutor.runOnKey(auditShardKey(globalKey, snapshot), () -> eventAuditRepository.save(EventAuditEntity.builder()
                .globalKey(globalKey)
                .serviceName(snapshot != null ? snapshot.service() : null)
                .integrationName(snapshot != null ? snapshot.integration() : null)
                .reason(DescriptionUtils.limit(reason))
                .headers(headers)
                .payload(payload)
                .yamlSnapshot(snapshot != null ? objectMapper.valueToTree(snapshot) : null)
                .build()));
    }

    private String auditShardKey(String globalKey, RouteModels.RouteSnapshot snapshot) {
        if (globalKey != null && !globalKey.isBlank()) {
            return globalKey;
        }
        return snapshot != null ? snapshot.service() + ":" + snapshot.integration() : null;
    }
}

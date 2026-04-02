package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.EventAuditEntity;
import ru.itmo.idempotency.core.repository.EventAuditRepository;

@Service
@RequiredArgsConstructor
public class EventAuditService {

    private final EventAuditRepository eventAuditRepository;
    private final CoreJsonSupport coreJsonSupport;

    @Transactional
    public EventAuditEntity save(String globalKey,
                                 RouteModels.RouteSnapshot route,
                                 String reason,
                                 JsonNode headers,
                                 JsonNode payload) {
        EventAuditEntity entity = EventAuditEntity.builder()
                .globalKey(globalKey)
                .serviceName(route != null ? route.service() : null)
                .integrationName(route != null ? route.integration() : null)
                .reason(reason)
                .headers(headers)
                .payload(payload)
                .yamlSnapshot(route != null ? coreJsonSupport.toJsonNode(route) : null)
                .build();
        return eventAuditRepository.save(entity);
    }
}

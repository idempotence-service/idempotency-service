package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.domain.ProcessingResult;

@Service
@RequiredArgsConstructor
public class InboundMessageTransactionalHandler {

    private final IdempotencyService idempotencyService;
    private final KafkaEventOutboxService kafkaEventOutboxService;
    private final EventAuditService eventAuditService;

    @Transactional
    public boolean saveUnique(String globalKey,
                              String uid,
                              RouteModels.RouteSnapshot route,
                              JsonNode userHeaders,
                              JsonNode payload) {
        if (!idempotencyService.saveIfAbsent(globalKey, uid, route, userHeaders, payload)) {
            return false;
        }
        if (route.requestOut() != null) {
            kafkaEventOutboxService.save(globalKey, route, ProcessingResult.SUCCESS, "Событие успешно получено");
        }
        eventAuditService.save(globalKey, route, AuditReasons.IDEMPOTENCY_PASSED, userHeaders, payload);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveDuplicate(String globalKey,
                              RouteModels.RouteSnapshot route,
                              JsonNode headers,
                              JsonNode payload) {
        if (route.requestOut() != null) {
            kafkaEventOutboxService.save(globalKey, route, ProcessingResult.FAIL, AuditReasons.IDEMPOTENCY_FAILED);
        }
        eventAuditService.save(globalKey, route, AuditReasons.IDEMPOTENCY_FAILED, headers, payload);
    }
}

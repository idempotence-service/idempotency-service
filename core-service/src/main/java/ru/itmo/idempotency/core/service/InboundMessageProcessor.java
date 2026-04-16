package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.messaging.MessageModels;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboundMessageProcessor {

    private final CoreJsonSupport coreJsonSupport;
    private final InboundMessageTransactionalHandler transactionalHandler;
    private final EventAuditService eventAuditService;

    public void handle(RouteModels.RouteSnapshot route, String rawMessage) {
        MessageModels.MessageEnvelope envelope;
        try {
            envelope = coreJsonSupport.parseEnvelope(rawMessage);
        } catch (IllegalArgumentException exception) {
            saveInvalidInbound(route, rawMessage);
            return;
        }

        Map<String, Object> headers = envelope.headers();
        JsonNode payload = envelope.payload();
        String uid = headers != null && headers.get("uid") != null ? String.valueOf(headers.get("uid")) : null;
        if (uid == null || uid.isBlank() || payload == null || payload.isNull()) {
            saveInvalidInbound(route, rawMessage);
            return;
        }

        String globalKey = route.service() + ":" + route.integration() + ":" + uid;
        JsonNode userHeaders = coreJsonSupport.headersWithoutUid(headers);
        try {
            transactionalHandler.saveUnique(globalKey, uid, route, userHeaders, payload);
        } catch (DataIntegrityViolationException exception) {
            transactionalHandler.saveDuplicate(globalKey, route, coreJsonSupport.toJsonNode(headers), payload);
        }
    }

    private void saveInvalidInbound(RouteModels.RouteSnapshot route, String rawMessage) {
        log.warn("Invalid inbound message received for route {}", route.integration());
        eventAuditService.save(null, route, AuditReasons.INVALID_INBOUND_EVENT, null, coreJsonSupport.safeRawPayload(rawMessage));
    }
}

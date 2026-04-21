package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReplyProcessor {

    private final CoreJsonSupport coreJsonSupport;
    private final EventAuditService eventAuditService;
    private final IdempotencySearchService idempotencySearchService;
    private final IdempotencyService idempotencyService;
    private final CoreMetrics coreMetrics;
    private final MdcContextSupport mdcContextSupport;

    public void handle(RouteModels.RouteSnapshot route, String rawMessage) {
        try (MdcContextSupport.Scope ignored = mdcContextSupport.open(null, null, route.integration(), null)) {
        MessageModels.MessageEnvelope envelope;
        try {
            envelope = coreJsonSupport.parseEnvelope(rawMessage);
        } catch (IllegalArgumentException exception) {
            saveInvalidReply(route, rawMessage);
            return;
        }

        Map<String, Object> headers = envelope.headers();
        JsonNode payload = envelope.payload();
        String globalKey = headers != null && headers.get("globalKey") != null ? String.valueOf(headers.get("globalKey")) : null;
        if (globalKey == null || globalKey.isBlank() || payload == null || payload.get("result") == null) {
            saveInvalidReply(route, rawMessage);
            return;
        }

        try (MdcContextSupport.Scope replyScope = mdcContextSupport.open(globalKey, null, route.integration(), null)) {
            processReply(route, globalKey, payload, coreJsonSupport.toJsonNode(headers));
        }
    }
    }

    @Transactional
    protected void processReply(RouteModels.RouteSnapshot route, String globalKey, JsonNode payload, JsonNode headers) {
        IdempotencyEntity entity = idempotencySearchService.acquireUniqueWaitIfLocked(globalKey).orElse(null);
        if (entity == null || entity.getStatus() != IdempotencyStatus.WAITING_ASYNC_RESPONSE) {
            eventAuditService.save(globalKey, route, AuditReasons.ORPHAN_REPLY, headers, payload);
            coreMetrics.recordAsyncReplyOrphan();
            return;
        }

        String result = payload.path("result").asText();
        String resultDescription = payload.path("resultDescription").asText(null);
        if ("SUCCESS".equalsIgnoreCase(result)) {
            idempotencyService.changeStatus(entity, IdempotencyStatus.COMMITTED, resultDescription);
            coreMetrics.recordAsyncReplySuccess();
            return;
        }

        if (payload.path("needResend").asBoolean(false)) {
            idempotencyService.changeStatus(
                    entity,
                    IdempotencyStatus.RESERVED,
                    "Система-получатель запросила повторную отправку события: " + resultDescription
            );
            coreMetrics.recordAsyncReplyResend();
            return;
        }

        idempotencyService.markAsError(entity, "Ошибка обработки события системой-получателем: " + resultDescription);
        coreMetrics.recordAsyncReplyFailure();
    }

    @Transactional
    protected void saveInvalidReply(RouteModels.RouteSnapshot route, String rawMessage) {
        log.warn("Invalid async reply received for route {}", route.integration());
        eventAuditService.save(null, route, AuditReasons.INVALID_RECEIVER_REPLY, null, coreJsonSupport.safeRawPayload(rawMessage));
        coreMetrics.recordAsyncReplyInvalid();
    }
}

package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncReplyProcessorTest {

    @Mock
    private CoreJsonSupport coreJsonSupport;
    @Mock
    private EventAuditService eventAuditService;
    @Mock
    private IdempotencySearchService idempotencySearchService;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private CoreMetrics coreMetrics;
    @Mock
    private MdcContextSupport mdcContextSupport;

    private CoreProperties coreProperties;

    @InjectMocks
    private AsyncReplyProcessor processor;

    private RouteModels.RouteSnapshot route;

    @BeforeEach
    void setUp() {
        coreProperties = new CoreProperties();
        processor = new AsyncReplyProcessor(
                coreJsonSupport,
                eventAuditService,
                idempotencySearchService,
                idempotencyService,
                coreProperties,
                coreMetrics,
                mdcContextSupport
        );
        route = new RouteModels.RouteSnapshot("svc", "int", null, null, null, null, true);
        when(mdcContextSupport.open(any(), any(), any(), any())).thenReturn(() -> {});
    }

    @Test
    void shouldRecordInvalidReplyOnParseFailure() {
        when(coreJsonSupport.parseEnvelope(anyString())).thenThrow(new IllegalArgumentException("bad"));
        when(coreJsonSupport.safeRawPayload(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

        processor.handle(route, "bad");

        verify(eventAuditService).save(eq(null), eq(route), eq(AuditReasons.INVALID_RECEIVER_REPLY), eq(null), any());
        verify(coreMetrics).recordAsyncReplyInvalid("int");
    }

    @Test
    void shouldRecordInvalidReplyWhenGlobalKeyMissing() {
        var payload = JsonNodeFactory.instance.objectNode().put("result", "SUCCESS");
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(new MessageModels.MessageEnvelope(Map.of(), payload));
        when(coreJsonSupport.safeRawPayload(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

        processor.handle(route, "{}");

        verify(coreMetrics).recordAsyncReplyInvalid("int");
    }

    @Test
    void shouldRecordInvalidReplyWhenResultMissing() {
        var payload = JsonNodeFactory.instance.objectNode();
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(
                new MessageModels.MessageEnvelope(Map.of("globalKey", "gk-1"), payload)
        );
        when(coreJsonSupport.safeRawPayload(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

        processor.handle(route, "{}");

        verify(coreMetrics).recordAsyncReplyInvalid("int");
    }

    @Test
    void shouldRecordOrphanReplyWhenEntityMissing() {
        stubValidEnvelope("FAIL");
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.empty());

        processor.handle(route, "{}");

        verify(eventAuditService).save(eq("gk-1"), eq(route), eq(AuditReasons.ORPHAN_REPLY), any(), any());
        verify(coreMetrics).recordAsyncReplyOrphan("int");
    }

    @Test
    void shouldRecordOrphanReplyWhenWrongStatus() {
        stubValidEnvelope("SUCCESS");
        IdempotencyEntity entity = entity(IdempotencyStatus.COMMITTED);
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));

        processor.handle(route, "{}");

        verify(coreMetrics).recordAsyncReplyOrphan("int");
    }

    @Test
    void shouldCommitOnSuccess() {
        stubValidEnvelope("SUCCESS");
        IdempotencyEntity entity = entity(IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));

        processor.handle(route, "{}");

        verify(idempotencyService).changeStatus(entity, IdempotencyStatus.COMMITTED, "desc");
        verify(coreMetrics).recordAsyncReplySuccess("int");
    }

    @Test
    void shouldScheduleResendWhenNeedResend() {
        stubValidEnvelopeWithResend();
        IdempotencyEntity entity = entity(IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        IdempotencyEntity reserved = entity(IdempotencyStatus.RESERVED);
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));
        when(idempotencyService.scheduleRetry(eq(entity), anyString(), any(Duration.class), eq(5))).thenReturn(reserved);

        processor.handle(route, "{}");

        verify(coreMetrics).recordAsyncReplyResend("int");
    }

    @Test
    void shouldRecordFailureWhenMaxAttemptsReachedOnResend() {
        stubValidEnvelopeWithResend();
        IdempotencyEntity entity = entity(IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        IdempotencyEntity error = entity(IdempotencyStatus.ERROR);
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));
        when(idempotencyService.scheduleRetry(eq(entity), anyString(), any(Duration.class), eq(5))).thenReturn(error);

        processor.handle(route, "{}");

        verify(coreMetrics).recordAsyncReplyFailure("int");
    }

    @Test
    void shouldMarkErrorOnFailWithoutResend() {
        stubValidEnvelope("FAIL");
        IdempotencyEntity entity = entity(IdempotencyStatus.WAITING_ASYNC_RESPONSE);
        when(idempotencySearchService.acquireUniqueWaitIfLocked("gk-1")).thenReturn(Optional.of(entity));

        processor.handle(route, "{}");

        verify(idempotencyService).markAsError(eq(entity), anyString());
        verify(coreMetrics).recordAsyncReplyFailure("int");
        verify(idempotencyService, never()).scheduleRetry(any(), anyString(), any(Duration.class), anyInt());
    }

    private void stubValidEnvelope(String result) {
        var payload = JsonNodeFactory.instance.objectNode()
                .put("result", result)
                .put("resultDescription", "desc");
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(
                new MessageModels.MessageEnvelope(Map.of("globalKey", "gk-1"), payload)
        );
        when(coreJsonSupport.toJsonNode(any())).thenReturn(JsonNodeFactory.instance.objectNode());
    }

    private void stubValidEnvelopeWithResend() {
        var payload = JsonNodeFactory.instance.objectNode()
                .put("result", "FAIL")
                .put("resultDescription", "retry")
                .put("needResend", true);
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(
                new MessageModels.MessageEnvelope(Map.of("globalKey", "gk-1"), payload)
        );
        when(coreJsonSupport.toJsonNode(any())).thenReturn(JsonNodeFactory.instance.objectNode());
    }

    private static IdempotencyEntity entity(IdempotencyStatus status) {
        return IdempotencyEntity.builder()
                .globalKey("gk-1")
                .status(status)
                .build();
    }
}

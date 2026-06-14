package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.messaging.MessageModels;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundMessageProcessorTest {

    @Mock
    private CoreJsonSupport coreJsonSupport;
    @Mock
    private InboundMessageTransactionalHandler transactionalHandler;
    @Mock
    private EventAuditService eventAuditService;
    @Mock
    private CoreMetrics coreMetrics;
    @Mock
    private MdcContextSupport mdcContextSupport;

    @InjectMocks
    private InboundMessageProcessor processor;

    private RouteModels.RouteSnapshot route;

    @BeforeEach
    void setUp() {
        route = new RouteModels.RouteSnapshot("sender-service", "system1-to-system2", null, null, null, null, true);
        when(mdcContextSupport.open(any(), any(), any(), any())).thenReturn(() -> {});
    }

    @Test
    void shouldSaveInvalidInboundOnParseFailure() {
        when(coreJsonSupport.parseEnvelope(anyString())).thenThrow(new IllegalArgumentException("bad json"));
        when(coreJsonSupport.safeRawPayload(anyString())).thenReturn(JsonNodeFactory.instance.objectNode().put("raw", "{}"));

        processor.handle(route, "{}");

        verify(eventAuditService).save(eq(null), eq(route), eq(AuditReasons.INVALID_INBOUND_EVENT), eq(null), any());
        verify(coreMetrics).recordInboundInvalid();
        verify(transactionalHandler, never()).saveUnique(anyString(), anyString(), any(), any(), any());
    }

    @Test
    void shouldSaveInvalidInboundWhenUidMissing() {
        MessageModels.MessageEnvelope envelope = new MessageModels.MessageEnvelope(
                Map.of(),
                JsonNodeFactory.instance.objectNode()
        );
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(envelope);
        when(coreJsonSupport.safeRawPayload(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

        processor.handle(route, "{}");

        verify(coreMetrics).recordInboundInvalid();
    }

    @Test
    void shouldSaveInvalidInboundWhenPayloadNull() {
        MessageModels.MessageEnvelope envelope = new MessageModels.MessageEnvelope(
                Map.of("uid", "u-1"),
                null
        );
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(envelope);
        when(coreJsonSupport.safeRawPayload(anyString())).thenReturn(JsonNodeFactory.instance.objectNode());

        processor.handle(route, "{}");

        verify(coreMetrics).recordInboundInvalid();
    }

    @Test
    void shouldSaveUniqueInboundMessage() {
        var payload = JsonNodeFactory.instance.objectNode().put("x", 1);
        MessageModels.MessageEnvelope envelope = new MessageModels.MessageEnvelope(Map.of("uid", "u-1"), payload);
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(envelope);
        when(coreJsonSupport.headersWithoutUid(any())).thenReturn(JsonNodeFactory.instance.objectNode());

        processor.handle(route, "{}");

        verify(transactionalHandler).saveUnique(
                eq("sender-service:system1-to-system2:u-1"),
                eq("u-1"),
                eq(route),
                any(),
                eq(payload)
        );
        verify(coreMetrics).recordInboundUnique();
    }

    @Test
    void shouldSaveDuplicateOnIntegrityViolation() {
        var payload = JsonNodeFactory.instance.objectNode().put("x", 1);
        MessageModels.MessageEnvelope envelope = new MessageModels.MessageEnvelope(Map.of("uid", "u-1"), payload);
        when(coreJsonSupport.parseEnvelope(anyString())).thenReturn(envelope);
        when(coreJsonSupport.headersWithoutUid(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(coreJsonSupport.toJsonNode(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(transactionalHandler).saveUnique(anyString(), anyString(), any(), any(), any());

        processor.handle(route, "{}");

        verify(transactionalHandler).saveDuplicate(
                eq("sender-service:system1-to-system2:u-1"),
                eq(route),
                any(),
                eq(payload)
        );
        verify(coreMetrics).recordInboundDuplicate();
    }
}

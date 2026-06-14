package ru.itmo.idempotency.sender.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderDispatchServiceTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;
    @Mock
    private SenderStateService senderStateService;

    @InjectMocks
    private SenderDispatchService senderDispatchService;

    @Test
    void shouldGenerateUidAndMergeHeaders() {
        RouteModels.RouteSnapshot route = route("localhost:9092", "sender.events.inbound");
        when(routeCatalog.getRequiredRoute("system1-to-system2")).thenReturn(route);

        String uid = senderDispatchService.send(
                "system1-to-system2",
                null,
                Map.of("traceId", "abc"),
                JsonNodeFactory.instance.objectNode().put("orderId", 1),
                1
        );

        Assertions.assertNotNull(uid);
        Assertions.assertFalse(uid.isBlank());

        ArgumentCaptor<Map<String, Object>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaJsonProducerRegistry).send(
                eq("localhost:9092"),
                eq("sender.events.inbound"),
                eq(uid),
                org.mockito.ArgumentMatchers.any(MessageModels.MessageEnvelope.class)
        );
        verify(senderStateService).recordSent(
                eq("system1-to-system2"),
                eq(uid),
                headersCaptor.capture(),
                org.mockito.ArgumentMatchers.any()
        );
        Assertions.assertEquals("abc", headersCaptor.getValue().get("traceId"));
        Assertions.assertEquals(uid, headersCaptor.getValue().get("uid"));
    }

    @Test
    void shouldSendDuplicateMessagesWhenRequested() {
        RouteModels.RouteSnapshot route = route("localhost:9092", "sender.events.inbound");
        when(routeCatalog.getRequiredRoute("system1-to-system2")).thenReturn(route);

        senderDispatchService.send(
                "system1-to-system2",
                "fixed-uid",
                Map.of(),
                JsonNodeFactory.instance.objectNode(),
                3
        );

        verify(kafkaJsonProducerRegistry, times(3)).send(
                eq("localhost:9092"),
                eq("sender.events.inbound"),
                eq("fixed-uid"),
                org.mockito.ArgumentMatchers.any(MessageModels.MessageEnvelope.class)
        );
        verify(senderStateService, times(3)).recordSent(
                eq("system1-to-system2"),
                eq("fixed-uid"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldThrowForUnknownIntegration() {
        when(routeCatalog.getRequiredRoute("missing")).thenThrow(new IllegalArgumentException("Route not found: missing"));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> senderDispatchService.send("missing", "uid-1", Map.of(), JsonNodeFactory.instance.objectNode(), 1)
        );
    }

    private static RouteModels.RouteSnapshot route(String host, String topic) {
        return new RouteModels.RouteSnapshot(
                "sender-service",
                "system1-to-system2",
                new RouteModels.RouteChannel(host, topic, null, 1, (short) 1),
                null,
                null,
                null,
                true
        );
    }
}

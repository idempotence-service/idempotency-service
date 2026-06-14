package ru.itmo.idempotency.receiver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiverReplyServiceTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReceiverReplyService receiverReplyService;

    @Test
    void shouldSendReplyToConfiguredTopic() {
        RouteModels.RouteSnapshot route = new RouteModels.RouteSnapshot(
                "sender-service",
                "system1-to-system2",
                null,
                null,
                null,
                new RouteModels.RouteChannel("localhost:9092", "receiver.events.reply", "group", 1, (short) 1),
                true
        );
        when(routeCatalog.getRequiredRoute("system1-to-system2")).thenReturn(route);
        when(objectMapper.valueToTree(org.mockito.ArgumentMatchers.any())).thenReturn(new ObjectMapper().createObjectNode());

        receiverReplyService.sendReply("system1-to-system2", "gk-1", "SUCCESS", false, "done");

        ArgumentCaptor<MessageModels.MessageEnvelope> envelopeCaptor = ArgumentCaptor.forClass(MessageModels.MessageEnvelope.class);
        verify(kafkaJsonProducerRegistry).send(
                eq("localhost:9092"),
                eq("receiver.events.reply"),
                eq("gk-1"),
                envelopeCaptor.capture()
        );
        Assertions.assertEquals("gk-1", envelopeCaptor.getValue().headers().get("globalKey"));
    }

    @Test
    void shouldNoOpWhenReplyOutMissing() {
        RouteModels.RouteSnapshot route = new RouteModels.RouteSnapshot(
                "sender-service",
                "system1-to-system2",
                null,
                null,
                null,
                null,
                true
        );
        when(routeCatalog.getRequiredRoute("system1-to-system2")).thenReturn(route);

        receiverReplyService.sendReply("system1-to-system2", "gk-1", "SUCCESS", false, "done");

        verifyNoInteractions(kafkaJsonProducerRegistry);
    }

    @Test
    void shouldThrowForUnknownIntegration() {
        when(routeCatalog.getRequiredRoute("missing")).thenThrow(new IllegalArgumentException("Route not found: missing"));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> receiverReplyService.sendReply("missing", "gk-1", "SUCCESS", false, "done")
        );
    }
}

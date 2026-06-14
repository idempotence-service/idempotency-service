package ru.itmo.idempotency.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class MessageModelsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConstructMessageEnvelope() {
        var payload = JsonNodeFactory.instance.objectNode().put("orderId", 42);
        var envelope = new MessageModels.MessageEnvelope(Map.of("uid", "source-1"), payload);

        Assertions.assertEquals("source-1", envelope.headers().get("uid"));
        Assertions.assertEquals(42, envelope.payload().get("orderId").asInt());
    }

    @Test
    void shouldRoundTripAsyncReplyPayloadThroughJackson() throws Exception {
        var original = new MessageModels.AsyncReplyPayload("SUCCESS", false, "done");

        String json = objectMapper.writeValueAsString(original);
        MessageModels.AsyncReplyPayload restored = objectMapper.readValue(json, MessageModels.AsyncReplyPayload.class);

        Assertions.assertEquals("SUCCESS", restored.result());
        Assertions.assertFalse(restored.needResend());
        Assertions.assertEquals("done", restored.resultDescription());
    }

    @Test
    void shouldRoundTripTechnicalResponsePayloadThroughJackson() throws Exception {
        var original = new MessageModels.TechnicalResponsePayload("FAIL", "error");

        String json = objectMapper.writeValueAsString(original);
        MessageModels.TechnicalResponsePayload restored = objectMapper.readValue(json, MessageModels.TechnicalResponsePayload.class);

        Assertions.assertEquals("FAIL", restored.result());
        Assertions.assertEquals("error", restored.resultDescription());
    }
}

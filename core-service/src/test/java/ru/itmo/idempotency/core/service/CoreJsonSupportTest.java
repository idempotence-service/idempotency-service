package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class CoreJsonSupportTest {

    private final CoreJsonSupport coreJsonSupport = new CoreJsonSupport(new ObjectMapper());

    @Test
    void shouldWrapRawPayloadWithoutParsing() {
        var node = coreJsonSupport.safeRawPayload("{\"orderId\":42}");

        Assertions.assertEquals("{\"orderId\":42}", node.get("raw").asText());
    }

    @Test
    void shouldWrapInvalidJsonAsRawString() {
        var node = coreJsonSupport.safeRawPayload("not-json");

        Assertions.assertEquals("not-json", node.get("raw").asText());
    }

    @Test
    void shouldParseValidEnvelope() {
        var envelope = coreJsonSupport.parseEnvelope("{\"headers\":{\"uid\":\"1\"},\"payload\":{\"a\":1}}");

        Assertions.assertEquals("1", envelope.headers().get("uid"));
        Assertions.assertEquals(1, envelope.payload().get("a").asInt());
    }

    @Test
    void shouldRejectInvalidEnvelope() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> coreJsonSupport.parseEnvelope("not-json"));
    }

    @Test
    void shouldStripUidFromHeaders() {
        var node = coreJsonSupport.headersWithoutUid(Map.of("uid", "1", "traceId", "abc"));

        Assertions.assertFalse(node.has("uid"));
        Assertions.assertEquals("abc", node.get("traceId").asText());
    }
}

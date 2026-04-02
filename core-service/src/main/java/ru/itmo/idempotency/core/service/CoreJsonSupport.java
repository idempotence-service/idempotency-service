package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.messaging.MessageModels;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CoreJsonSupport {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public MessageModels.MessageEnvelope parseEnvelope(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, MessageModels.MessageEnvelope.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Failed to parse message envelope", exception);
        }
    }

    public RouteModels.RouteSnapshot parseSnapshot(JsonNode snapshot) {
        return objectMapper.convertValue(snapshot, RouteModels.RouteSnapshot.class);
    }

    public JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    public Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, MAP_TYPE);
    }

    public JsonNode safeRawPayload(String rawMessage) {
        if (rawMessage == null) {
            return JsonNodeFactory.instance.nullNode();
        }
        return JsonNodeFactory.instance.textNode(rawMessage);
    }
}

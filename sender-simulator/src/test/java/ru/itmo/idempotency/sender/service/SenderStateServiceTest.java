package ru.itmo.idempotency.sender.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.itmo.idempotency.sender.config.SenderProperties;

import java.util.Map;

class SenderStateServiceTest {

    @Test
    void shouldKeepTotalsWhileTrimmingHistory() {
        SenderProperties properties = new SenderProperties();
        properties.getState().setHistoryLimit(2);

        SenderStateService stateService = new SenderStateService(properties);
        stateService.recordSent("system1-to-system2", "uid-1", Map.of("uid", "uid-1"), JsonNodeFactory.instance.objectNode());
        stateService.recordSent("system1-to-system2", "uid-2", Map.of("uid", "uid-2"), JsonNodeFactory.instance.objectNode());
        stateService.recordSent("system1-to-system2", "uid-3", Map.of("uid", "uid-3"), JsonNodeFactory.instance.objectNode());

        Assertions.assertEquals(3, stateService.stats().totalSent());
        Assertions.assertEquals(2, stateService.sentMessages().size());
        Assertions.assertEquals("uid-2", stateService.sentMessages().getFirst().uid());
        Assertions.assertEquals("uid-3", stateService.sentMessages().getLast().uid());
    }
}

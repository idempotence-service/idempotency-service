package ru.itmo.idempotency.receiver.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.itmo.idempotency.receiver.config.ReceiverProperties;

import java.util.Map;

class ReceiverStateServiceTest {

    @Test
    void shouldDetectDuplicateFingerprints() {
        ReceiverStateService stateService = new ReceiverStateService(new ReceiverProperties());

        ReceiverStateService.ReceivedMessage first = stateService.record(
                "system1-to-system2",
                "gk-1",
                Map.of("globalKey", "gk-1"),
                JsonNodeFactory.instance.objectNode()
        );
        ReceiverStateService.ReceivedMessage second = stateService.record(
                "system1-to-system2",
                "gk-1",
                Map.of("globalKey", "gk-1"),
                JsonNodeFactory.instance.objectNode()
        );

        Assertions.assertFalse(first.duplicate());
        Assertions.assertTrue(second.duplicate());
        Assertions.assertEquals(2, stateService.stats().totalReceived());
        Assertions.assertEquals(1, stateService.stats().totalDuplicates());
    }

    @Test
    void shouldTrimHistoryAndExposeStats() {
        ReceiverProperties properties = new ReceiverProperties();
        properties.getState().setHistoryLimit(2);
        ReceiverStateService stateService = new ReceiverStateService(properties);

        stateService.record("system1-to-system2", "gk-1", Map.of(), JsonNodeFactory.instance.objectNode());
        stateService.record("system1-to-system2", "gk-2", Map.of(), JsonNodeFactory.instance.objectNode());
        stateService.record("system1-to-system2", "gk-3", Map.of(), JsonNodeFactory.instance.objectNode());

        Assertions.assertEquals(3, stateService.stats().totalReceived());
        Assertions.assertEquals(2, stateService.messages().size());
        Assertions.assertEquals("gk-2", stateService.messages().getFirst().globalKey());
    }

    @Test
    void shouldResetState() {
        ReceiverStateService stateService = new ReceiverStateService(new ReceiverProperties());
        stateService.record("system1-to-system2", "gk-1", Map.of(), JsonNodeFactory.instance.objectNode());
        stateService.setMode("system1-to-system2", ReceiverMode.MANUAL);

        stateService.reset();

        Assertions.assertEquals(0, stateService.stats().totalReceived());
        Assertions.assertTrue(stateService.messages().isEmpty());
        Assertions.assertEquals(ReceiverMode.AUTO_SUCCESS, stateService.modeFor("system1-to-system2"));
    }
}

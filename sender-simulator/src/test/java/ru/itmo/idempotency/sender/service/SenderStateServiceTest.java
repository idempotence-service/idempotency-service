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

    @Test
    void shouldRecordReplyAndKeepTotalsWhileTrimmingHistory() {
        SenderProperties properties = new SenderProperties();
        properties.getState().setHistoryLimit(2);

        SenderStateService stateService = new SenderStateService(properties);
        stateService.recordReply("system1-to-system2", "gk-1", Map.of("globalKey", "gk-1"), JsonNodeFactory.instance.objectNode().put("result", "SUCCESS"));
        stateService.recordReply("system1-to-system2", "gk-2", Map.of("globalKey", "gk-2"), JsonNodeFactory.instance.objectNode().put("result", "SUCCESS"));
        stateService.recordReply("system1-to-system2", "gk-3", Map.of("globalKey", "gk-3"), JsonNodeFactory.instance.objectNode().put("result", "FAIL"));

        Assertions.assertEquals(3, stateService.stats().totalReplies());
        Assertions.assertEquals(2, stateService.receivedReplies().size());
        Assertions.assertEquals("gk-2", stateService.receivedReplies().getFirst().globalKey());
        Assertions.assertEquals("gk-3", stateService.receivedReplies().getLast().globalKey());
    }

    @Test
    void shouldResetCountersAndHistory() {
        SenderStateService stateService = new SenderStateService(new SenderProperties());
        stateService.recordSent("system1-to-system2", "uid-1", Map.of("uid", "uid-1"), JsonNodeFactory.instance.objectNode());
        stateService.recordReply("system1-to-system2", "gk-1", Map.of("globalKey", "gk-1"), JsonNodeFactory.instance.objectNode());

        stateService.reset();

        SenderStateService.StateStats stats = stateService.stats();
        Assertions.assertEquals(0, stats.totalSent());
        Assertions.assertEquals(0, stats.totalReplies());
        Assertions.assertTrue(stateService.sentMessages().isEmpty());
        Assertions.assertTrue(stateService.receivedReplies().isEmpty());
    }

    @Test
    void shouldTrackTotalsWithoutHistoryWhenStoreHistoryDisabled() {
        SenderProperties properties = new SenderProperties();
        properties.getState().setStoreHistory(false);

        SenderStateService stateService = new SenderStateService(properties);
        stateService.recordSent("system1-to-system2", "uid-1", Map.of("uid", "uid-1"), JsonNodeFactory.instance.objectNode());
        stateService.recordReply("system1-to-system2", "gk-1", Map.of("globalKey", "gk-1"), JsonNodeFactory.instance.objectNode());

        SenderStateService.StateStats stats = stateService.stats();
        Assertions.assertEquals(1, stats.totalSent());
        Assertions.assertEquals(1, stats.totalReplies());
        Assertions.assertFalse(stats.historyEnabled());
        Assertions.assertTrue(stateService.sentMessages().isEmpty());
        Assertions.assertTrue(stateService.receivedReplies().isEmpty());
    }
}

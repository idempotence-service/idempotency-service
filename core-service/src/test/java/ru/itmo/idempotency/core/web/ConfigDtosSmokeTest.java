package ru.itmo.idempotency.core.web;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

class ConfigDtosSmokeTest {

    @Test
    void shouldInstantiateAllRecords() {
        ConfigDtos.ChannelInfo channel = new ConfigDtos.ChannelInfo("localhost:9092", "topic", "group", 1, (short) 1);
        ConfigDtos.SchedulerConfig scheduler = new ConfigDtos.SchedulerConfig(5L, 5L, 30L, 86400L, 100);
        ConfigDtos.ListenerConfig listener = new ConfigDtos.ListenerConfig(1, 1);
        ConfigDtos.ResilienceConfig resilience = new ConfigDtos.ResilienceConfig(10L, 10L, 60L, 30L, 5);
        ConfigDtos.CleanupConfig cleanup = new ConfigDtos.CleanupConfig(604800L, 500);
        ConfigDtos.FullConfig full = new ConfigDtos.FullConfig(scheduler, listener, resilience, cleanup);
        ConfigDtos.IntegrationInfo integration = new ConfigDtos.IntegrationInfo(
                "int", "svc", channel, channel, channel, channel, true
        );

        Assertions.assertNotNull(full.scheduler());
        Assertions.assertEquals("topic", integration.inbound().topic());
    }
}

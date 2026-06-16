package ru.itmo.idempotency.common.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import ru.itmo.idempotency.common.config.RouteModels;

import java.util.List;

class KafkaTopicProvisionerTest {

    private static EmbeddedKafkaBroker embeddedKafkaBroker;
    private final KafkaTopicProvisioner provisioner = new KafkaTopicProvisioner();

    @BeforeAll
    static void startKafka() {
        embeddedKafkaBroker = new EmbeddedKafkaKraftBroker(1, 1, "existing-topic");
        embeddedKafkaBroker.afterPropertiesSet();
    }

    @AfterAll
    static void stopKafka() {
        if (embeddedKafkaBroker != null) {
            embeddedKafkaBroker.destroy();
        }
    }

    @Test
    void shouldCreateTopicsWithCustomPartitions() throws Exception {
        String bootstrapServers = embeddedKafkaBroker.getBrokersAsString();
        RouteModels.RouteChannel channel = new RouteModels.RouteChannel(
                bootstrapServers,
                "custom-partitioned-topic",
                "group",
                3,
                (short) 1
        );

        provisioner.ensureTopics(List.of(channel));

        try (AdminClient adminClient = AdminClient.create(KafkaClientSupport.adminProperties(bootstrapServers))) {
            DescribeTopicsResult result = adminClient.describeTopics(List.of("custom-partitioned-topic"));
            TopicDescription description = result.topicNameValues().get("custom-partitioned-topic").get();
            Assertions.assertEquals(3, description.partitions().size());
        }
    }

    @Test
    void shouldBeIdempotentWhenTopicAlreadyExists() {
        String bootstrapServers = embeddedKafkaBroker.getBrokersAsString();
        RouteModels.RouteChannel channel = new RouteModels.RouteChannel(
                bootstrapServers,
                "existing-topic",
                "group",
                1,
                (short) 1
        );

        Assertions.assertDoesNotThrow(() -> provisioner.ensureTopics(List.of(channel)));
    }

    @Test
    void shouldIgnoreNullChannels() {
        java.util.ArrayList<RouteModels.RouteChannel> channels = new java.util.ArrayList<>();
        channels.add(null);
        Assertions.assertDoesNotThrow(() -> provisioner.ensureTopics(channels));
    }
}

package ru.itmo.idempotency.core;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1)
class CoreKafkaProvisioningIntegrationTest {

    private static final String INBOUND_TOPIC = "sender.events.inbound.scaled";
    private static final String REQUEST_OUT_TOPIC = "sender.events.request-out.scaled";
    private static final String RECEIVER_UNIQUE_TOPIC = "receiver.events.unique.scaled";
    private static final String RECEIVER_REPLY_TOPIC = "receiver.events.reply.scaled";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-topic-provisioning;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @Test
    void shouldProvisionConfiguredTopicPartitions() throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                embeddedKafkaBroker.getBrokersAsString()
        ))) {
            waitForPartitions(adminClient, INBOUND_TOPIC, 3);
            waitForPartitions(adminClient, REQUEST_OUT_TOPIC, 3);
            waitForPartitions(adminClient, RECEIVER_UNIQUE_TOPIC, 3);
            waitForPartitions(adminClient, RECEIVER_REPLY_TOPIC, 3);
        }
    }

    private void waitForPartitions(AdminClient adminClient, String topic, int expectedPartitions) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                TopicDescription description = adminClient.describeTopics(java.util.List.of(topic))
                        .allTopicNames()
                        .get()
                        .get(topic);
                if (description.partitions().size() == expectedPartitions) {
                    return;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(100);
        }
        Assertions.fail("Topic " + topic + " was not provisioned with " + expectedPartitions + " partitions");
    }

    private static String createRoutesFile(String bootstrapServers) {
        try {
            Path file = Files.createTempFile("routes-topics-test-", ".yaml");
            Files.writeString(file, """
                    service:
                      name: sender-service

                    kafka:
                      routes:
                        system1-to-system2:
                          sender:
                            producer:
                              host: %s
                              topic: %s
                              partitions: 3
                              replicationFactor: 1
                            consumer:
                              host: %s
                              topic: %s
                              group: sender-test-replies
                              partitions: 3
                              replicationFactor: 1
                          receiver:
                            producer:
                              host: %s
                              topic: %s
                              partitions: 3
                              replicationFactor: 1
                            consumer:
                              host: %s
                              topic: %s
                              group: core-test-replies
                              partitions: 3
                              replicationFactor: 1
                          idempotency:
                            enabled: true
                    """.formatted(
                    bootstrapServers,
                    INBOUND_TOPIC,
                    bootstrapServers,
                    REQUEST_OUT_TOPIC,
                    bootstrapServers,
                    RECEIVER_UNIQUE_TOPIC,
                    bootstrapServers,
                    RECEIVER_REPLY_TOPIC
            ));
            return file.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

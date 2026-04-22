package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.service.CleanupService;
import ru.itmo.idempotency.core.service.ReceiverDispatchProcessor;
import ru.itmo.idempotency.core.service.RequestDispatchProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "sender.events.inbound",
        "sender.events.request-out",
        "receiver.events.unique",
        "receiver.events.reply"
})
class CoreStatusLifecycleIntegrationTest {

    private static final String INBOUND_TOPIC = "sender.events.inbound";
    private static final String REPLY_OUT_TOPIC = "receiver.events.reply";
    private static final String RECEIVER_UNIQUE_TOPIC = "receiver.events.unique";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;

    @Autowired
    private RequestDispatchProcessor requestDispatchProcessor;

    @Autowired
    private ReceiverDispatchProcessor receiverDispatchProcessor;

    @Autowired
    private CleanupService cleanupService;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> receiverUniqueConsumer;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-status-lifecycle;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
        registry.add("app.resilience.delivery-retry-delay", () -> "5s");
    }

    @BeforeEach
    void setUp() {
        receiverUniqueConsumer = createConsumer("core-status-receiver", RECEIVER_UNIQUE_TOPIC);
    }

    @AfterEach
    void tearDown() {
        receiverUniqueConsumer.close();
        idempotencyRepository.deleteAll();
        kafkaEventOutboxRepository.deleteAll();
    }

    @Test
    void shouldReturnReservedEventBackToDeliveryQueueOnResendReply() throws Exception {
        String uid = UUID.randomUUID().toString();
        String globalKey = "sender-service:system1-to-system2:" + uid;

        kafkaJsonProducerRegistry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                INBOUND_TOPIC,
                uid,
                new MessageModels.MessageEnvelope(Map.of("uid", uid), objectMapper.readTree("{\"orderId\":42}"))
        );

        waitForCondition(() -> idempotencyRepository.findById(globalKey).isPresent());
        waitForCondition(() -> kafkaEventOutboxRepository.count() == 1);

        requestDispatchProcessor.processBatch(10);
        receiverDispatchProcessor.processBatch(10);
        consumeMessages(receiverUniqueConsumer, RECEIVER_UNIQUE_TOPIC, 1, Duration.ofSeconds(10));

        kafkaJsonProducerRegistry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                REPLY_OUT_TOPIC,
                globalKey,
                new MessageModels.MessageEnvelope(
                        Map.of("globalKey", globalKey),
                        objectMapper.valueToTree(new MessageModels.AsyncReplyPayload("FAIL", true, "Нужна повторная отправка"))
                )
        );

        waitForCondition(() -> idempotencyRepository.findById(globalKey)
                .map(entity -> entity.getStatus() == IdempotencyStatus.RESERVED && entity.getRetryCount() == 1)
                .orElse(false));

        Assertions.assertEquals(0, receiverDispatchProcessor.processBatch(10));

        IdempotencyEntity scheduledForRetry = idempotencyRepository.findById(globalKey).orElseThrow();
        scheduledForRetry.setNextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        idempotencyRepository.saveAndFlush(scheduledForRetry);

        Assertions.assertEquals(1, receiverDispatchProcessor.processBatch(10));
        List<MessageModels.MessageEnvelope> resentMessages = consumeMessages(receiverUniqueConsumer, RECEIVER_UNIQUE_TOPIC, 1, Duration.ofSeconds(10));
        Assertions.assertEquals(uid, resentMessages.getFirst().headers().get("uid"));

        kafkaJsonProducerRegistry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                REPLY_OUT_TOPIC,
                globalKey,
                new MessageModels.MessageEnvelope(
                        Map.of("globalKey", globalKey),
                        objectMapper.valueToTree(new MessageModels.AsyncReplyPayload("SUCCESS", false, "Готово"))
                )
        );

        waitForCondition(() -> idempotencyRepository.findById(globalKey)
                .map(entity -> entity.getStatus() == IdempotencyStatus.COMMITTED)
                .orElse(false));
    }

    @Test
    void shouldCleanupOnlyExpiredCommittedEvents() throws Exception {
        String oldUid = UUID.randomUUID().toString();
        String freshUid = UUID.randomUUID().toString();

        idempotencyRepository.saveAndFlush(IdempotencyEntity.builder()
                .globalKey("sender-service:system1-to-system2:" + oldUid)
                .sourceUid(oldUid)
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.readTree("{}"))
                .headers(objectMapper.readTree("{}"))
                .payload(objectMapper.readTree("{}"))
                .status(IdempotencyStatus.COMMITTED)
                .createDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
                .updateDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build());

        idempotencyRepository.saveAndFlush(IdempotencyEntity.builder()
                .globalKey("sender-service:system1-to-system2:" + freshUid)
                .sourceUid(freshUid)
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.readTree("{}"))
                .headers(objectMapper.readTree("{}"))
                .payload(objectMapper.readTree("{}"))
                .status(IdempotencyStatus.COMMITTED)
                .createDate(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .updateDate(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .build());

        int deleted = cleanupService.cleanupCommitted();

        Assertions.assertEquals(1, deleted);
        Assertions.assertFalse(idempotencyRepository.findById("sender-service:system1-to-system2:" + oldUid).isPresent());
        Assertions.assertTrue(idempotencyRepository.findById("sender-service:system1-to-system2:" + freshUid).isPresent());
    }

    private Consumer<String, String> createConsumer(String groupId, String topic) {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(groupId + "-" + UUID.randomUUID(), "false", embeddedKafkaBroker);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                new StringDeserializer()
        );
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, true, topic);
        return consumer;
    }

    private List<MessageModels.MessageEnvelope> consumeMessages(Consumer<String, String> consumer,
                                                                String topic,
                                                                int expectedCount,
                                                                Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        List<MessageModels.MessageEnvelope> result = new ArrayList<>();
        while (System.currentTimeMillis() < deadline && result.size() < expectedCount) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records.records(topic)) {
                result.add(objectMapper.readValue(record.value(), MessageModels.MessageEnvelope.class));
            }
        }
        if (result.size() < expectedCount) {
            Assertions.fail("Expected " + expectedCount + " messages but got " + result.size());
        }
        return result;
    }

    private void waitForCondition(Check check) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (check.ok()) {
                return;
            }
            Thread.sleep(100);
        }
        Assertions.fail("Condition was not met within timeout");
    }

    private static String createRoutesFile(String bootstrapServers) {
        try {
            Path file = Files.createTempFile("routes-test-", ".yaml");
            Files.writeString(file, """
                    service:
                      name: sender-service

                    kafka:
                      routes:
                        system1-to-system2:
                          sender:
                            producer:
                              host: %s
                              topic: sender.events.inbound
                            consumer:
                              host: %s
                              topic: sender.events.request-out
                              group: sender-test-replies
                          receiver:
                            producer:
                              host: %s
                              topic: receiver.events.unique
                            consumer:
                              host: %s
                              topic: receiver.events.reply
                              group: core-test-replies
                          idempotency:
                            enabled: true
                    """.formatted(bootstrapServers, bootstrapServers, bootstrapServers, bootstrapServers));
            return file.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean ok() throws Exception;
    }
}

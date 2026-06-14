package ru.itmo.idempotency.receiver;

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
import ru.itmo.idempotency.receiver.service.ReceiverMode;
import ru.itmo.idempotency.receiver.service.ReceiverStateService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static ru.itmo.idempotency.common.testsupport.RoutesTestSupport.createRoutesFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "receiver.events.unique",
        "receiver.events.reply"
})
class ReceiverSimulatorModesIntegrationTest {

    private static final String RECEIVER_UNIQUE_TOPIC = "receiver.events.unique";
    private static final String RECEIVER_REPLY_TOPIC = "receiver.events.reply";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;

    @Autowired
    private ReceiverStateService receiverStateService;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> replyConsumer;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @BeforeEach
    void setUp() {
        replyConsumer = createConsumer("receiver-modes", RECEIVER_REPLY_TOPIC);
        receiverStateService.reset();
    }

    @AfterEach
    void tearDown() {
        replyConsumer.close();
        receiverStateService.reset();
    }

    @Test
    void shouldAutoReplyWithSuccess() throws Exception {
        receiverStateService.setMode("system1-to-system2", ReceiverMode.AUTO_SUCCESS);
        String globalKey = publishMessage();

        MessageModels.MessageEnvelope reply = consumeMessages(replyConsumer, RECEIVER_REPLY_TOPIC, 1, Duration.ofSeconds(10)).getFirst();

        Assertions.assertEquals(globalKey, reply.headers().get("globalKey"));
        Assertions.assertEquals("SUCCESS", reply.payload().path("result").asText());
        Assertions.assertFalse(reply.payload().path("needResend").asBoolean());
    }

    @Test
    void shouldAutoReplyWithFailWithoutResend() throws Exception {
        receiverStateService.setMode("system1-to-system2", ReceiverMode.AUTO_FAIL_NO_RESEND);
        String globalKey = publishMessage();

        MessageModels.MessageEnvelope reply = consumeMessages(replyConsumer, RECEIVER_REPLY_TOPIC, 1, Duration.ofSeconds(10)).getFirst();

        Assertions.assertEquals(globalKey, reply.headers().get("globalKey"));
        Assertions.assertEquals("FAIL", reply.payload().path("result").asText());
        Assertions.assertFalse(reply.payload().path("needResend").asBoolean());
    }

    @Test
    void shouldNotAutoReplyInManualMode() throws Exception {
        receiverStateService.setMode("system1-to-system2", ReceiverMode.MANUAL);
        publishMessage();

        waitForNoMessages(Duration.ofSeconds(2));
    }

    private String publishMessage() throws Exception {
        String globalKey = "sender-service:system1-to-system2:" + UUID.randomUUID();
        kafkaJsonProducerRegistry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                RECEIVER_UNIQUE_TOPIC,
                globalKey,
                new MessageModels.MessageEnvelope(
                        Map.of("globalKey", globalKey, "uid", "source-1"),
                        objectMapper.readTree("{\"amount\":100}")
                )
        );
        waitForCondition(() -> !receiverStateService.messages().isEmpty());
        return globalKey;
    }

    private Consumer<String, String> createConsumer(String groupId, String topic) {
        var properties = KafkaTestUtils.consumerProps(groupId + "-" + UUID.randomUUID(), "false", embeddedKafkaBroker);
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

    private void waitForNoMessages(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = replyConsumer.poll(Duration.ofMillis(200));
            if (!records.isEmpty()) {
                Assertions.fail("Expected no replies but received " + records.count());
            }
            Thread.sleep(100);
        }
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

    @FunctionalInterface
    private interface Check {
        boolean ok() throws Exception;
    }
}

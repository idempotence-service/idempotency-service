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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.receiver.service.ReceiverMode;
import ru.itmo.idempotency.receiver.service.ReceiverStateService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "receiver.events.unique",
        "receiver.events.reply"
})
class ReceiverSimulatorIntegrationTest {

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

    @Autowired
    private MockMvc mockMvc;

    private Consumer<String, String> replyConsumer;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @BeforeEach
    void setUp() {
        replyConsumer = createConsumer("receiver-replies", RECEIVER_REPLY_TOPIC);
        receiverStateService.reset();
    }

    @AfterEach
    void tearDown() {
        replyConsumer.close();
        receiverStateService.reset();
    }

    @Test
    void shouldExposeDuplicateDeliveriesThroughReceiverState() throws Exception {
        String globalKey = "sender-service:system1-to-system2:" + UUID.randomUUID();
        MessageModels.MessageEnvelope envelope = new MessageModels.MessageEnvelope(
                Map.of("globalKey", globalKey, "uid", "source-1"),
                objectMapper.readTree("{\"amount\":100}")
        );

        kafkaJsonProducerRegistry.send(embeddedKafkaBroker.getBrokersAsString(), RECEIVER_UNIQUE_TOPIC, globalKey, envelope);
        kafkaJsonProducerRegistry.send(embeddedKafkaBroker.getBrokersAsString(), RECEIVER_UNIQUE_TOPIC, globalKey, envelope);

        waitForCondition(() -> receiverStateService.messages().size() == 2);

        mockMvc.perform(get("/api/receiver/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].duplicate").value(false))
                .andExpect(jsonPath("$.data[1].duplicate").value(true));
    }

    @Test
    void shouldSendRetryReplyWhenReceiverModeRequiresResend() throws Exception {
        receiverStateService.setMode("system1-to-system2", ReceiverMode.AUTO_FAIL_RESEND);
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

        List<MessageModels.MessageEnvelope> replies = consumeMessages(replyConsumer, RECEIVER_REPLY_TOPIC, 1, Duration.ofSeconds(10));
        MessageModels.MessageEnvelope reply = replies.getFirst();

        Assertions.assertEquals(globalKey, reply.headers().get("globalKey"));
        Assertions.assertEquals("FAIL", reply.payload().path("result").asText());
        Assertions.assertTrue(reply.payload().path("needResend").asBoolean());
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

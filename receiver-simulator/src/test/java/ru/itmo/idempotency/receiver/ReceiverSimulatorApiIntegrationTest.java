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
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.receiver.service.ReceiverStateService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.itmo.idempotency.common.testsupport.RoutesTestSupport.createRoutesFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "receiver.events.unique",
        "receiver.events.reply"
})
class ReceiverSimulatorApiIntegrationTest {

    private static final String RECEIVER_REPLY_TOPIC = "receiver.events.reply";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

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
        replyConsumer = createConsumer("receiver-api", RECEIVER_REPLY_TOPIC);
        receiverStateService.reset();
    }

    @AfterEach
    void tearDown() {
        replyConsumer.close();
        receiverStateService.reset();
    }

    @Test
    void shouldExposeStatsAndModeEndpoints() throws Exception {
        mockMvc.perform(get("/api/receiver/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReceived").value(0));

        mockMvc.perform(post("/api/receiver/mode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"integration":"system1-to-system2","mode":"MANUAL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));
    }

    @Test
    void shouldSendManualReplyAndResetState() throws Exception {
        String globalKey = "sender-service:system1-to-system2:" + UUID.randomUUID();

        mockMvc.perform(post("/api/receiver/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "integration": "system1-to-system2",
                                  "globalKey": "%s",
                                  "result": "SUCCESS",
                                  "needResend": false,
                                  "resultDescription": "manual"
                                }
                                """.formatted(globalKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        MessageModels.MessageEnvelope reply = consumeMessages(replyConsumer, RECEIVER_REPLY_TOPIC, 1, Duration.ofSeconds(10)).getFirst();
        Assertions.assertEquals(globalKey, reply.headers().get("globalKey"));
        Assertions.assertEquals("SUCCESS", reply.payload().path("result").asText());

        mockMvc.perform(delete("/api/receiver/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ok"));

        mockMvc.perform(get("/api/receiver/stats"))
                .andExpect(jsonPath("$.data.totalReceived").value(0));
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
}

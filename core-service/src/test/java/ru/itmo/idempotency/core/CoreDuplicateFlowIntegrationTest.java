package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.repository.EventAuditRepository;
import ru.itmo.idempotency.core.service.AuditReasons;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.itmo.idempotency.common.testsupport.RoutesTestSupport.createRoutesFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "sender.events.inbound",
        "sender.events.request-out",
        "receiver.events.unique",
        "receiver.events.reply"
})
class CoreDuplicateFlowIntegrationTest {

    private static final String INBOUND_TOPIC = "sender.events.inbound";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventAuditRepository eventAuditRepository;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-duplicate-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @AfterEach
    void tearDown() {
        eventAuditRepository.deleteAll();
    }

    @Test
    void shouldRecordDuplicateAuditAndExposeCount() throws Exception {
        String uid = UUID.randomUUID().toString();
        MessageModels.MessageEnvelope envelope = new MessageModels.MessageEnvelope(
                Map.of("uid", uid),
                objectMapper.readTree("{\"orderId\":42}")
        );

        kafkaJsonProducerRegistry.send(embeddedKafkaBroker.getBrokersAsString(), INBOUND_TOPIC, uid, envelope);
        kafkaJsonProducerRegistry.send(embeddedKafkaBroker.getBrokersAsString(), INBOUND_TOPIC, uid, envelope);

        waitForCondition(() -> eventAuditRepository.countByReason(AuditReasons.IDEMPOTENCY_FAILED) == 1);

        mockMvc.perform(get("/get-duplicate-count")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(1));
    }

    private void waitForCondition(Check check) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
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

package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.idempotency.core.domain.EventAuditEntity;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.EventAuditRepository;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.service.AuditReasons;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class CoreManualReviewDuplicateApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventAuditRepository eventAuditRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-manual-review-dup;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @AfterEach
    void tearDown() {
        eventAuditRepository.deleteAll();
        idempotencyRepository.deleteAll();
    }

    @Test
    void shouldExposeDuplicateEventsAndCounts() throws Exception {
        seedAudit(AuditReasons.IDEMPOTENCY_FAILED, "dup-key-1");
        seedAudit(AuditReasons.ASYNC_REPLY_TIMEOUT, "timeout-key-1");

        mockMvc.perform(get("/get-duplicate-events")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].globalKey").value("dup-key-1"));

        mockMvc.perform(get("/get-duplicate-count")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(get("/get-timeout-count")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void shouldReturnTechnicalErrorWhenEventNotFound() throws Exception {
        mockMvc.perform(get("/get-event-by-id")
                        .header("Authorization", "Bearer operator-token")
                        .param("globalKey", "missing-key"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TECHNICAL_ERROR_02"));
    }

    @Test
    void shouldReturnAlreadyRestartedMessage() throws Exception {
        String globalKey = "sender-service:system1-to-system2:" + UUID.randomUUID();
        idempotencyRepository.saveAndFlush(IdempotencyEntity.builder()
                .globalKey(globalKey)
                .sourceUid("uid-1")
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.readTree("{}"))
                .headers(objectMapper.readTree("{}"))
                .payload(objectMapper.readTree("{}"))
                .status(IdempotencyStatus.RESERVED)
                .createDate(OffsetDateTime.now(ZoneOffset.UTC))
                .updateDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build());

        mockMvc.perform(post("/restart-event")
                        .header("Authorization", "Bearer operator-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"globalKey\":\"" + globalKey + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Задача уже перезапущена"));
    }

    private void seedAudit(String reason, String globalKey) throws Exception {
        eventAuditRepository.saveAndFlush(EventAuditEntity.builder()
                .globalKey(globalKey)
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .reason(reason)
                .headers(objectMapper.readTree("{}"))
                .payload(objectMapper.readTree("{}"))
                .yamlSnapshot(objectMapper.readTree("{}"))
                .createDate(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }
}

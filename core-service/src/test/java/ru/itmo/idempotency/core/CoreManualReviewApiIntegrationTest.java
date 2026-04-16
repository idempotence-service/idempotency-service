package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.junit.jupiter.api.AfterEach;
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
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "sender.events.inbound",
        "sender.events.request-out",
        "receiver.events.unique",
        "receiver.events.reply"
})
class CoreManualReviewApiIntegrationTest {

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-manual-review;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @AfterEach
    void tearDown() {
        idempotencyRepository.deleteAll();
    }

    @Test
    void shouldRequireAuthorizationForManualReviewApi() throws Exception {
        mockMvc.perform(get("/get-error-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TECHNICAL_ERROR_01"));
    }

    @Test
    void shouldExposeErrorEventsAndEventCard() throws Exception {
        String globalKey = saveErrorEvent();

        mockMvc.perform(get("/get-error-events")
                        .header("Authorization", "Bearer operator-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].globalKey").value(globalKey));

        mockMvc.perform(get("/get-event-by-id")
                        .header("Authorization", "Bearer operator-token")
                        .param("globalKey", globalKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.globalKey").value(globalKey))
                .andExpect(jsonPath("$.data.status").value("ERROR"));
    }

    @Test
    void shouldRestartErrorEventThroughManualReviewApi() throws Exception {
        String globalKey = saveErrorEvent();

        mockMvc.perform(post("/restart-event")
                        .header("Authorization", "Bearer operator-token")
                        .contentType("application/json")
                        .content("{\"globalKey\":\"" + globalKey + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Задача успешно перезапущена"));

        IdempotencyEntity entity = idempotencyRepository.findById(globalKey).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(IdempotencyStatus.RESERVED, entity.getStatus());
    }

    @Test
    void shouldReturnValidationErrorForUnsupportedSortDirection() throws Exception {
        mockMvc.perform(get("/get-error-events")
                        .header("Authorization", "Bearer operator-token")
                        .param("sort", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private String saveErrorEvent() throws Exception {
        String uid = UUID.randomUUID().toString();
        String globalKey = "sender-service:system1-to-system2:" + uid;
        idempotencyRepository.saveAndFlush(IdempotencyEntity.builder()
                .globalKey(globalKey)
                .sourceUid(uid)
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.readTree("{}"))
                .headers(objectMapper.readTree("{}"))
                .payload(objectMapper.readTree("{\"orderId\":42}"))
                .status(IdempotencyStatus.ERROR)
                .statusDescription("Ошибка доставки")
                .createDate(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5))
                .updateDate(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .build());
        return globalKey;
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
}

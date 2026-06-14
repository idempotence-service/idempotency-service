package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.domain.ProcessingResult;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.service.KafkaEventOutboxSearchService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;
import static ru.itmo.idempotency.common.testsupport.RoutesTestSupport.createRoutesFile;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "sender.events.inbound",
        "sender.events.request-out",
        "receiver.events.unique",
        "receiver.events.reply"
})
class CoreOutboxSearchIntegrationTest {

    @Autowired
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;

    @Autowired
    private KafkaEventOutboxSearchService kafkaEventOutboxSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-outbox-search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @AfterEach
    void tearDown() {
        kafkaEventOutboxRepository.deleteAll();
    }

    @Test
    void shouldAcquireFirstAvailableNewOutboxRow() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        KafkaEventOutboxEntity saved = kafkaEventOutboxRepository.saveAndFlush(KafkaEventOutboxEntity.builder()
                .globalKey("sender-service:system1-to-system2:uid-1")
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.createObjectNode())
                .status(OutboxStatus.NEW)
                .result(ProcessingResult.SUCCESS)
                .retryCount(0)
                .nextAttemptDate(now.minusSeconds(1))
                .build());

        Optional<KafkaEventOutboxEntity> acquired = kafkaEventOutboxSearchService.acquireFirstNotLocked();

        Assertions.assertTrue(acquired.isPresent());
        Assertions.assertEquals(saved.getId(), acquired.get().getId());
    }
}

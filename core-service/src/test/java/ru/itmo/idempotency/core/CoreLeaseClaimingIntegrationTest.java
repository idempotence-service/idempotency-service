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
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;
import ru.itmo.idempotency.core.domain.OutboxStatus;
import ru.itmo.idempotency.core.domain.ProcessingResult;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.service.IdempotencyService;
import ru.itmo.idempotency.core.service.KafkaEventOutboxService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1)
class CoreLeaseClaimingIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private KafkaEventOutboxService kafkaEventOutboxService;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-lease-claiming;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @AfterEach
    void tearDown() {
        kafkaEventOutboxRepository.deleteAll();
        idempotencyRepository.deleteAll();
    }

    @Test
    void shouldReclaimExpiredDeliveryLeaseAndRejectPreviousOwnerCompletion() throws Exception {
        String globalKey = "sender-service:system1-to-system2:lease-1";
        idempotencyRepository.saveAndFlush(IdempotencyEntity.builder()
                .globalKey(globalKey)
                .sourceUid("lease-1")
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.readTree("{}"))
                .headers(objectMapper.readTree("{}"))
                .payload(objectMapper.readTree("{}"))
                .status(IdempotencyStatus.RESERVED)
                .retryCount(0)
                .nextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1))
                .build());

        Assertions.assertTrue(idempotencyService.claimNextReserved("worker-a", Duration.ofSeconds(30)).isPresent());
        Assertions.assertTrue(idempotencyService.claimNextReserved("worker-b", Duration.ofSeconds(30)).isEmpty());

        IdempotencyEntity entity = idempotencyRepository.findById(globalKey).orElseThrow();
        entity.setLeaseUntil(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        idempotencyRepository.saveAndFlush(entity);

        Assertions.assertTrue(idempotencyService.claimNextReserved("worker-b", Duration.ofSeconds(30)).isPresent());
        Assertions.assertFalse(idempotencyService.completeClaimedDelivery(globalKey, "worker-a", IdempotencyStatus.COMMITTED, null));
        Assertions.assertTrue(idempotencyService.completeClaimedDelivery(globalKey, "worker-b", IdempotencyStatus.COMMITTED, null));

        IdempotencyEntity completed = idempotencyRepository.findById(globalKey).orElseThrow();
        Assertions.assertEquals(IdempotencyStatus.COMMITTED, completed.getStatus());
        Assertions.assertNull(completed.getOwnerId());
        Assertions.assertNull(completed.getLeaseUntil());
    }

    @Test
    void shouldReclaimExpiredOutboxLeaseAndRejectPreviousOwnerCompletion() throws Exception {
        KafkaEventOutboxEntity outboxEntity = kafkaEventOutboxRepository.saveAndFlush(KafkaEventOutboxEntity.builder()
                .globalKey("sender-service:system1-to-system2:lease-outbox")
                .serviceName("sender-service")
                .integrationName("system1-to-system2")
                .yamlSnapshot(objectMapper.readTree("{}"))
                .status(OutboxStatus.NEW)
                .result(ProcessingResult.SUCCESS)
                .resultDescription("ok")
                .retryCount(0)
                .nextAttemptDate(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1))
                .build());

        Assertions.assertTrue(kafkaEventOutboxService.claimNextNew("worker-a", Duration.ofSeconds(30)).isPresent());
        Assertions.assertTrue(kafkaEventOutboxService.claimNextNew("worker-b", Duration.ofSeconds(30)).isEmpty());

        KafkaEventOutboxEntity stored = kafkaEventOutboxRepository.findById(outboxEntity.getId()).orElseThrow();
        stored.setLeaseUntil(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        kafkaEventOutboxRepository.saveAndFlush(stored);

        Assertions.assertTrue(kafkaEventOutboxService.claimNextNew("worker-b", Duration.ofSeconds(30)).isPresent());
        Assertions.assertFalse(kafkaEventOutboxService.completeClaimedDispatch(outboxEntity.getId(), "worker-a", null));
        Assertions.assertTrue(kafkaEventOutboxService.completeClaimedDispatch(outboxEntity.getId(), "worker-b", null));

        KafkaEventOutboxEntity completed = kafkaEventOutboxRepository.findById(outboxEntity.getId()).orElseThrow();
        Assertions.assertEquals(OutboxStatus.DONE, completed.getStatus());
        Assertions.assertNull(completed.getOwnerId());
        Assertions.assertNull(completed.getLeaseUntil());
    }

    private static String createRoutesFile(String bootstrapServers) {
        try {
            Path file = Files.createTempFile("routes-lease-test-", ".yaml");
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

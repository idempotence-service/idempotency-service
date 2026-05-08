package ru.itmo.idempotency.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;
import ru.itmo.idempotency.core.repository.IdempotencyRepository;
import ru.itmo.idempotency.core.repository.KafkaEventOutboxRepository;
import ru.itmo.idempotency.core.service.ReceiverDispatchProcessor;
import ru.itmo.idempotency.core.service.ReplyTimeoutRecoveryService;
import ru.itmo.idempotency.core.service.RequestDispatchProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
class CoreAsyncReplyTimeoutIntegrationTest {

    private static final String INBOUND_TOPIC = "sender.events.inbound";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaJsonProducerRegistry kafkaJsonProducerRegistry;

    @Autowired
    private RequestDispatchProcessor requestDispatchProcessor;

    @Autowired
    private ReceiverDispatchProcessor receiverDispatchProcessor;

    @Autowired
    private ReplyTimeoutRecoveryService replyTimeoutRecoveryService;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private KafkaEventOutboxRepository kafkaEventOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-async-reply-timeout;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
        registry.add("app.resilience.reply-timeout", () -> "200ms");
        registry.add("app.resilience.delivery-retry-delay", () -> "0s");
    }

    @Test
    void shouldReturnTimedOutAsyncReplyToReservedStatus() throws Exception {
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

        waitForCondition(() -> idempotencyRepository.findById(globalKey)
                .map(entity -> entity.getStatus() == IdempotencyStatus.WAITING_ASYNC_RESPONSE)
                .orElse(false));

        Thread.sleep(300);
        Assertions.assertEquals(1, replyTimeoutRecoveryService.processBatch(10));

        waitForCondition(() -> idempotencyRepository.findById(globalKey)
                .map(entity -> entity.getStatus() == IdempotencyStatus.RESERVED && entity.getRetryCount() == 1)
                .orElse(false));
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
            Path file = Files.createTempFile("routes-timeout-test-", ".yaml");
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

package ru.itmo.idempotency.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.itmo.idempotency.core.service.CoreMetrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.springframework.kafka.test.EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1)
class CorePrometheusMetricsIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CoreMetrics coreMetrics;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:core-prometheus-metrics;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON\\;CREATE DOMAIN IF NOT EXISTS TIMESTAMPTZ AS TIMESTAMP WITH TIME ZONE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.routes-file", () -> createRoutesFile(System.getProperty(SPRING_EMBEDDED_KAFKA_BROKERS)));
    }

    @Test
    void shouldExposeCustomPrometheusMetrics() {
        coreMetrics.recordInboundUnique();
        coreMetrics.recordDeliverySuccess(Duration.ofMillis(25));
        coreMetrics.recordAsyncReplyTimeout();

        String body = testRestTemplate.getForObject("/actuator/prometheus", String.class);

        Assertions.assertNotNull(body);
        Assertions.assertTrue(body.contains("idempotency_inbound_total_total") || body.contains("idempotency_inbound_total"));
        Assertions.assertTrue(body.contains("idempotency_async_reply_total_total") || body.contains("idempotency_async_reply_total"));
        Assertions.assertTrue(body.contains("idempotency_delivery_duration_seconds_bucket"));
        Assertions.assertTrue(body.contains("idempotency_queue_backlog"));
    }

    private static String createRoutesFile(String bootstrapServers) {
        try {
            Path file = Files.createTempFile("routes-prometheus-test-", ".yaml");
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

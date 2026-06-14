package ru.itmo.idempotency.common.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaJsonProducerRegistryTest {

    private static EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeAll
    static void startKafka() {
        embeddedKafkaBroker = new EmbeddedKafkaKraftBroker(1, 1, "producer-test-topic");
        embeddedKafkaBroker.afterPropertiesSet();
    }

    @AfterAll
    static void stopKafka() {
        if (embeddedKafkaBroker != null) {
            embeddedKafkaBroker.destroy();
        }
    }

    @Test
    void shouldSendMessageViaEmbeddedKafka() {
        KafkaJsonProducerRegistry registry = new KafkaJsonProducerRegistry(new ObjectMapper());

        Assertions.assertDoesNotThrow(() -> registry.send(
                embeddedKafkaBroker.getBrokersAsString(),
                "producer-test-topic",
                "key-1",
                Map.of("value", "ok")
        ));

        registry.destroy();
    }

    @Test
    void shouldWrapJsonProcessingException() throws Exception {
        ObjectMapper mapper = mock(ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenThrow(jsonProcessingException("bad"));
        KafkaJsonProducerRegistry registry = new KafkaJsonProducerRegistry(mapper);

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () ->
                registry.send("localhost:9092", "topic", "k", Map.of("a", 1))
        );

        Assertions.assertTrue(exception.getMessage().contains("Kafka send failed"));
        registry.destroy();
    }

    @Test
    void shouldWrapExecutionException() throws Exception {
        KafkaJsonProducerRegistry registry = new KafkaJsonProducerRegistry(new ObjectMapper());
        injectTemplate(registry, failingFuture(new ExecutionException("fail", new RuntimeException("broker"))));

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () ->
                registry.send("localhost:9092", "topic", "k", Map.of("a", 1))
        );

        Assertions.assertTrue(exception.getMessage().contains("Kafka send failed"));
        registry.destroy();
    }

    @Test
    void shouldWrapTimeoutException() throws Exception {
        KafkaJsonProducerRegistry registry = new KafkaJsonProducerRegistry(new ObjectMapper());
        injectTemplate(registry, new CompletableFuture<>());

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () ->
                registry.send("localhost:9092", "topic", "k", Map.of("a", 1))
        );

        Assertions.assertInstanceOf(TimeoutException.class, exception.getCause());
        registry.destroy();
    }

    @Test
    void shouldRestoreInterruptOnInterruptedException() throws Exception {
        KafkaJsonProducerRegistry registry = new KafkaJsonProducerRegistry(new ObjectMapper());
        injectTemplate(registry, interruptedFuture());

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () ->
                registry.send("localhost:9092", "topic", "k", Map.of("a", 1))
        );

        Assertions.assertTrue(exception.getMessage().contains("interrupted"));
        Assertions.assertTrue(Thread.interrupted());
        registry.destroy();
    }

    private static JsonProcessingException jsonProcessingException(String message) {
        return new JsonProcessingException(message) {
            private static final long serialVersionUID = 1L;
        };
    }

    @SuppressWarnings("unchecked")
    private static void injectTemplate(KafkaJsonProducerRegistry registry, CompletableFuture<SendResult<String, String>> future)
            throws Exception {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(anyString(), anyString(), anyString())).thenReturn(future);
        Field field = KafkaJsonProducerRegistry.class.getDeclaredField("templates");
        field.setAccessible(true);
        Map<String, KafkaTemplate<String, String>> templates = new ConcurrentHashMap<>();
        templates.put("localhost:9092", template);
        field.set(registry, templates);
    }

    private static CompletableFuture<SendResult<String, String>> interruptedFuture() {
        return new CompletableFuture<>() {
            @Override
            public SendResult<String, String> get(long timeout, TimeUnit unit) throws InterruptedException {
                throw new InterruptedException("stopped");
            }
        };
    }

    private static <T> CompletableFuture<T> failingFuture(Exception exception) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(exception);
        return future;
    }
}

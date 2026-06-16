package ru.itmo.idempotency.core.service;

import org.apache.kafka.common.errors.NetworkException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;

class KafkaExceptionClassifierTest {

    @Test
    void shouldTreatRetriableExceptionAsRetriable() {
        Assertions.assertTrue(KafkaExceptionClassifier.isRetriable(new NetworkException("retry")));
    }

    @Test
    void shouldTreatTimeoutExceptionAsRetriable() {
        Assertions.assertTrue(KafkaExceptionClassifier.isRetriable(new TimeoutException("timeout")));
    }

    @Test
    void shouldTreatWrappedRetriableCauseAsRetriable() {
        Assertions.assertTrue(KafkaExceptionClassifier.isRetriable(new RuntimeException(new TimeoutException("timeout"))));
    }

    @Test
    void shouldTreatNonRetriableExceptionAsNonRetriable() {
        Assertions.assertFalse(KafkaExceptionClassifier.isRetriable(new IllegalStateException("fatal")));
    }
}

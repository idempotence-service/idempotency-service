package ru.itmo.idempotency.core.service;

import org.apache.kafka.common.errors.RetriableException;

import java.util.concurrent.TimeoutException;

public final class KafkaExceptionClassifier {

    private KafkaExceptionClassifier() {
    }

    public static boolean isRetriable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RetriableException || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

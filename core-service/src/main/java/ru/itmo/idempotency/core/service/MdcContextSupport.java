package ru.itmo.idempotency.core.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MdcContextSupport {

    public Scope open(String globalKey, String uid, String integration, String ownerId) {
        Map<String, String> additions = new LinkedHashMap<>();
        putIfPresent(additions, "globalKey", globalKey);
        putIfPresent(additions, "uid", uid);
        putIfPresent(additions, "integration", integration);
        putIfPresent(additions, "ownerId", ownerId);
        return open(additions);
    }

    public Scope open(Map<String, String> additions) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (previous != null) {
            MDC.setContextMap(previous);
        } else {
            MDC.clear();
        }
        additions.forEach(MDC::put);
        return () -> {
            if (previous == null || previous.isEmpty()) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        };
    }

    private void putIfPresent(Map<String, String> additions, String key, String value) {
        if (value != null && !value.isBlank()) {
            additions.put(key, value);
        }
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}

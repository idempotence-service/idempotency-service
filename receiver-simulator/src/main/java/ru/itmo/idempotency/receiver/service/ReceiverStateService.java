package ru.itmo.idempotency.receiver.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.common.util.BoundedHistory;
import ru.itmo.idempotency.receiver.config.ReceiverProperties;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ReceiverStateService {

    private final ReceiverProperties receiverProperties;
    private final BoundedHistory<ReceivedMessage> messages;
    private final Map<String, ReceiverMode> modes = new ConcurrentHashMap<>();
    private final Set<Long> seenGlobalKeyFingerprints = ConcurrentHashMap.newKeySet();
    private final LongAdder totalReceived = new LongAdder();
    private final LongAdder totalDuplicates = new LongAdder();

    public ReceiverStateService(ReceiverProperties receiverProperties) {
        this.receiverProperties = receiverProperties;
        this.messages = new BoundedHistory<>(receiverProperties.getState().getHistoryLimit());
    }

    public ReceivedMessage record(String integration, String globalKey, Map<String, Object> headers, JsonNode payload) {
        totalReceived.increment();
        boolean duplicate = !seenGlobalKeyFingerprints.add(fingerprint(globalKey));
        if (duplicate) {
            totalDuplicates.increment();
        }
        ReceivedMessage message = new ReceivedMessage(integration, globalKey, duplicate, headers, payload, OffsetDateTime.now(ZoneOffset.UTC));
        if (receiverProperties.getState().isStoreHistory()) {
            messages.add(message);
        }
        return message;
    }

    public List<ReceivedMessage> messages() {
        return messages.snapshot();
    }

    public List<ReceivedMessage> messages(String since) {
        if (since == null || since.isEmpty()) {
            return messages.snapshot();
        }
        try {
            OffsetDateTime sinceTime = OffsetDateTime.parse(since);
            return messages.snapshot().stream()
                    .filter(msg -> msg.timestamp() != null && !msg.timestamp().isBefore(sinceTime))
                    .toList();
        } catch (DateTimeParseException e) {
            return messages.snapshot();
        }
    }

    public ReceiverMode modeFor(String integration) {
        return modes.getOrDefault(integration, ReceiverMode.AUTO_SUCCESS);
    }

    public void setMode(String integration, ReceiverMode mode) {
        modes.put(integration, mode);
    }

    public void reset() {
        messages.clear();
        modes.clear();
        seenGlobalKeyFingerprints.clear();
        totalReceived.reset();
        totalDuplicates.reset();
    }

    public StateStats stats() {
        return stats(null);
    }

    public StateStats stats(String since) {
        if (since == null || since.isEmpty()) {
            return new StateStats(
                    totalReceived.sum(),
                    totalDuplicates.sum(),
                    messages.size(),
                    seenGlobalKeyFingerprints.size(),
                    receiverProperties.getState().isStoreHistory(),
                    receiverProperties.getState().getHistoryLimit()
            );
        }
        
        try {
            OffsetDateTime sinceTime = OffsetDateTime.parse(since);
            long receivedCount = messages.snapshot().stream()
                    .filter(msg -> msg.timestamp() != null && !msg.timestamp().isBefore(sinceTime))
                    .count();
            long duplicateCount = messages.snapshot().stream()
                    .filter(msg -> msg.timestamp() != null && !msg.timestamp().isBefore(sinceTime) && msg.duplicate())
                    .count();
            return new StateStats(
                    receivedCount,
                    duplicateCount,
                    messages.size(),
                    seenGlobalKeyFingerprints.size(),
                    receiverProperties.getState().isStoreHistory(),
                    receiverProperties.getState().getHistoryLimit()
            );
        } catch (DateTimeParseException e) {
            return new StateStats(
                    totalReceived.sum(),
                    totalDuplicates.sum(),
                    messages.size(),
                    seenGlobalKeyFingerprints.size(),
                    receiverProperties.getState().isStoreHistory(),
                    receiverProperties.getState().getHistoryLimit()
            );
        }
    }

    public record ReceivedMessage(String integration,
                                  String globalKey,
                                  boolean duplicate,
                                  Map<String, Object> headers,
                                  JsonNode payload,
                                  OffsetDateTime timestamp) {
    }

    public record StateStats(long totalReceived,
                             long totalDuplicates,
                             int messageHistorySize,
                             int trackedKeyCount,
                             boolean historyEnabled,
                             int historyLimit) {
    }

    // The simulator keeps only fingerprints to reduce memory pressure during stress runs.
    private long fingerprint(String globalKey) {
        long hash = 0xcbf29ce484222325L;
        String value = globalKey == null ? "" : globalKey;
        for (int index = 0; index < value.length(); index++) {
            hash ^= value.charAt(index);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}

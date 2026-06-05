package ru.itmo.idempotency.sender.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.common.util.BoundedHistory;
import ru.itmo.idempotency.sender.config.SenderProperties;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

@Service
public class SenderStateService {

    private final SenderProperties senderProperties;
    private final BoundedHistory<ObservedMessage> sentMessages;
    private final BoundedHistory<ObservedMessage> receivedReplies;
    private final LongAdder sentTotal = new LongAdder();
    private final LongAdder replyTotal = new LongAdder();

    public SenderStateService(SenderProperties senderProperties) {
        this.senderProperties = senderProperties;
        int historyLimit = senderProperties.getState().getHistoryLimit();
        this.sentMessages = new BoundedHistory<>(historyLimit);
        this.receivedReplies = new BoundedHistory<>(historyLimit);
    }

    public void recordSent(String integration, String uid, Map<String, Object> headers, JsonNode payload) {
        sentTotal.increment();
        if (!senderProperties.getState().isStoreHistory()) {
            return;
        }
        sentMessages.add(new ObservedMessage(integration, uid, null, headers, payload, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public void recordReply(String integration, String globalKey, Map<String, Object> headers, JsonNode payload) {
        replyTotal.increment();
        if (!senderProperties.getState().isStoreHistory()) {
            return;
        }
        receivedReplies.add(new ObservedMessage(integration, null, globalKey, headers, payload, OffsetDateTime.now(ZoneOffset.UTC)));
    }

    public List<ObservedMessage> sentMessages() {
        return sentMessages.snapshot();
    }

    public List<ObservedMessage> receivedReplies() {
        return receivedReplies.snapshot();
    }

    public StateStats stats() {
        return new StateStats(
                sentTotal.sum(),
                replyTotal.sum(),
                sentMessages.size(),
                receivedReplies.size(),
                senderProperties.getState().isStoreHistory(),
                senderProperties.getState().getHistoryLimit()
        );
    }

    public void reset() {
        sentMessages.clear();
        receivedReplies.clear();
        sentTotal.reset();
        replyTotal.reset();
    }

    public record ObservedMessage(String integration,
                                  String uid,
                                  String globalKey,
                                  Map<String, Object> headers,
                                  JsonNode payload,
                                  OffsetDateTime timestamp) {
    }

    public record StateStats(long totalSent,
                             long totalReplies,
                             int sentHistorySize,
                             int replyHistorySize,
                             boolean historyEnabled,
                             int historyLimit) {
    }
}

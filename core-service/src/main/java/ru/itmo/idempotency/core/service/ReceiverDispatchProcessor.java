package ru.itmo.idempotency.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.IdempotencyEntity;
import ru.itmo.idempotency.core.domain.IdempotencyStatus;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Service
public class ReceiverDispatchProcessor {

    private final IdempotencyService idempotencyService;
    private final CoreJsonSupport coreJsonSupport;
    private final KafkaJsonProducerRegistry kafkaJsonProducerRegistry;
    private final CoreProperties coreProperties;
    private final CoreMetrics coreMetrics;
    private final MdcContextSupport mdcContextSupport;

    public ReceiverDispatchProcessor(IdempotencyService idempotencyService,
                                     CoreJsonSupport coreJsonSupport,
                                     KafkaJsonProducerRegistry kafkaJsonProducerRegistry,
                                     CoreProperties coreProperties,
                                     CoreMetrics coreMetrics,
                                     MdcContextSupport mdcContextSupport) {
        this.idempotencyService = idempotencyService;
        this.coreJsonSupport = coreJsonSupport;
        this.kafkaJsonProducerRegistry = kafkaJsonProducerRegistry;
        this.coreProperties = coreProperties;
        this.coreMetrics = coreMetrics;
        this.mdcContextSupport = mdcContextSupport;
    }

    public int processBatch(int batchSize) {
        int processed = 0;
        while (processed < batchSize) {
            if (!processSingle()) {
                break;
            }
            processed++;
        }
        return processed;
    }

    private boolean processSingle() {
        String ownerId = coreProperties.getInstanceId();
        IdempotencyEntity entity = idempotencyService
                .claimNextReserved(ownerId, coreProperties.getResilience().getLeaseDuration())
                .orElse(null);
        if (entity == null) {
            return false;
        }

        RouteModels.RouteSnapshot snapshot = coreJsonSupport.parseSnapshot(entity.getYamlSnapshot());
        try (MdcContextSupport.Scope ignored = mdcContextSupport.open(
                entity.getGlobalKey(),
                entity.getSourceUid(),
                snapshot.integration(),
                ownerId
        )) {
            long startedAt = System.nanoTime();
            if (snapshot.replyIn() == null) {
                if (!idempotencyService.failClaimedDelivery(
                        entity.getGlobalKey(),
                        ownerId,
                        "Не найден канал reply_in в сохраненном yaml_snapshot"
                )) {
                    log.warn("Lost ownership of {} before schema validation error", entity.getGlobalKey());
                    coreMetrics.recordDeliveryOwnershipLost();
                } else {
                    coreMetrics.recordDeliveryFailure(Duration.ofNanos(System.nanoTime() - startedAt));
                }
                return true;
            }

            try {
                kafkaJsonProducerRegistry.send(
                        snapshot.replyIn().bootstrapServers(),
                        snapshot.replyIn().topic(),
                        entity.getGlobalKey(),
                        new MessageModels.MessageEnvelope(mergeHeaders(entity), entity.getPayload())
                );

                Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
                if (snapshot.replyOut() == null) {
                    if (!idempotencyService.completeClaimedDelivery(entity.getGlobalKey(), ownerId, IdempotencyStatus.COMMITTED, null)) {
                        log.warn("Lost ownership of {} before marking as committed", entity.getGlobalKey());
                        coreMetrics.recordDeliveryOwnershipLost();
                    } else {
                        coreMetrics.recordDeliverySuccess(duration);
                    }
                } else {
                    if (!idempotencyService.completeClaimedDelivery(entity.getGlobalKey(), ownerId, IdempotencyStatus.WAITING_ASYNC_RESPONSE, null)) {
                        log.warn("Lost ownership of {} before waiting async reply", entity.getGlobalKey());
                        coreMetrics.recordDeliveryOwnershipLost();
                    } else {
                        coreMetrics.recordDeliverySuccess(duration);
                    }
                }
            } catch (Exception exception) {
                Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
                String description = "Произошла ошибка при отправке события системе-получателю в "
                        + snapshot.replyIn().topic() + ": " + exception.getMessage();
                if (KafkaExceptionClassifier.isRetriable(exception)) {
                    if (!idempotencyService.retryClaimedDelivery(
                            entity.getGlobalKey(),
                            ownerId,
                            description,
                            coreProperties.getResilience().getDeliveryRetryDelay(),
                            coreProperties.getResilience().getMaxAttempts()
                    )) {
                        log.warn("Lost ownership of {} before retry scheduling", entity.getGlobalKey());
                        coreMetrics.recordDeliveryOwnershipLost();
                    } else {
                        coreMetrics.recordDeliveryRetry(duration);
                    }
                    log.warn("Temporary delivery issue for {}", entity.getGlobalKey(), exception);
                    return true;
                }

                if (!idempotencyService.failClaimedDelivery(entity.getGlobalKey(), ownerId, description)) {
                    log.warn("Lost ownership of {} before marking error", entity.getGlobalKey());
                    coreMetrics.recordDeliveryOwnershipLost();
                } else {
                    coreMetrics.recordDeliveryFailure(duration);
                }
                log.warn("Permanent delivery issue for {}", entity.getGlobalKey(), exception);
            }
        }

        return true;
    }

    private Map<String, Object> mergeHeaders(IdempotencyEntity entity) {
        ObjectNode headers = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        headers.put("globalKey", entity.getGlobalKey());
        headers.put("uid", entity.getSourceUid());
        headers.put("service", entity.getServiceName());
        headers.put("integration", entity.getIntegrationName());

        Iterator<Map.Entry<String, JsonNode>> fields = entity.getHeaders().fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            headers.set(field.getKey(), field.getValue());
        }

        return coreJsonSupport.toMap(headers);
    }
}

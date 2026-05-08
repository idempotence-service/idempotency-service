package ru.itmo.idempotency.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.kafka.KafkaJsonProducerRegistry;
import ru.itmo.idempotency.common.messaging.MessageModels;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.domain.KafkaEventOutboxEntity;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class RequestDispatchProcessor {

    private final KafkaEventOutboxService kafkaEventOutboxService;
    private final CoreJsonSupport coreJsonSupport;
    private final KafkaJsonProducerRegistry kafkaJsonProducerRegistry;
    private final CoreProperties coreProperties;
    private final CoreMetrics coreMetrics;
    private final MdcContextSupport mdcContextSupport;

    public RequestDispatchProcessor(KafkaEventOutboxService kafkaEventOutboxService,
                                    CoreJsonSupport coreJsonSupport,
                                    KafkaJsonProducerRegistry kafkaJsonProducerRegistry,
                                    CoreProperties coreProperties,
                                    CoreMetrics coreMetrics,
                                    MdcContextSupport mdcContextSupport) {
        this.kafkaEventOutboxService = kafkaEventOutboxService;
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
        KafkaEventOutboxEntity outboxEntity = kafkaEventOutboxService
                .claimNextNew(ownerId, coreProperties.getResilience().getLeaseDuration())
                .orElse(null);
        if (outboxEntity == null) {
            return false;
        }

        RouteModels.RouteSnapshot snapshot = coreJsonSupport.parseSnapshot(outboxEntity.getYamlSnapshot());
        try (MdcContextSupport.Scope ignored = mdcContextSupport.open(
                outboxEntity.getGlobalKey(),
                null,
                snapshot.integration(),
                ownerId
        )) {
            Duration duration = null;
            long startedAt = System.nanoTime();
            if (snapshot.requestOut() == null) {
                if (!kafkaEventOutboxService.failClaimedDispatch(
                        outboxEntity.getId(),
                        ownerId,
                        "Не найден канал request_out в сохраненном yaml_snapshot"
                )) {
                    log.warn("Lost ownership of technical response outbox {} before schema validation error", outboxEntity.getId());
                    coreMetrics.recordOutboxOwnershipLost();
                }
                coreMetrics.recordOutboxFailure(Duration.ofNanos(System.nanoTime() - startedAt));
                return true;
            }

            try {
                kafkaJsonProducerRegistry.send(
                        snapshot.requestOut().bootstrapServers(),
                        snapshot.requestOut().topic(),
                        outboxEntity.getGlobalKey(),
                        new MessageModels.MessageEnvelope(
                                Map.of("globalKey", outboxEntity.getGlobalKey()),
                                coreJsonSupport.toJsonNode(new MessageModels.TechnicalResponsePayload(
                                        outboxEntity.getResult().name(),
                                        outboxEntity.getResultDescription()
                                ))
                        )
                );
                duration = Duration.ofNanos(System.nanoTime() - startedAt);
                if (!kafkaEventOutboxService.completeClaimedDispatch(outboxEntity.getId(), ownerId, null)) {
                    log.warn("Lost ownership of technical response outbox {} before completion", outboxEntity.getId());
                    coreMetrics.recordOutboxOwnershipLost();
                } else {
                    coreMetrics.recordOutboxSuccess(duration);
                }
            } catch (Exception exception) {
                duration = Duration.ofNanos(System.nanoTime() - startedAt);
                String description = "Произошла ошибка при отправке ответа в " + snapshot.requestOut().topic() + ": " + exception.getMessage();
                if (KafkaExceptionClassifier.isRetriable(exception)) {
                    if (!kafkaEventOutboxService.retryClaimedDispatch(
                            outboxEntity.getId(),
                            ownerId,
                            description,
                            coreProperties.getResilience().getOutboxRetryDelay(),
                            coreProperties.getResilience().getMaxAttempts()
                    )) {
                        log.warn("Lost ownership of technical response outbox {} before retry scheduling", outboxEntity.getId());
                        coreMetrics.recordOutboxOwnershipLost();
                    } else {
                        coreMetrics.recordOutboxRetry(duration);
                    }
                    log.warn("Temporary technical response dispatch failure for {}", outboxEntity.getGlobalKey(), exception);
                    return true;
                }

                if (!kafkaEventOutboxService.failClaimedDispatch(outboxEntity.getId(), ownerId, description)) {
                    log.warn("Lost ownership of technical response outbox {} before marking error", outboxEntity.getId());
                    coreMetrics.recordOutboxOwnershipLost();
                } else {
                    coreMetrics.recordOutboxFailure(duration);
                }
                log.warn("Permanent technical response dispatch failure for {}", outboxEntity.getGlobalKey(), exception);
            }
        }
        return true;
    }
}

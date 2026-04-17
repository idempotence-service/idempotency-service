package ru.itmo.idempotency.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.core.config.CoreProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreScheduler {

    private final ReceiverDispatchProcessor receiverDispatchProcessor;
    private final RequestDispatchProcessor requestDispatchProcessor;
    private final CoreProperties coreProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.delivery-fixed-delay:5s}")
    public void dispatchToReceiver() {
        int processed = receiverDispatchProcessor.processBatch(coreProperties.getScheduler().getBatchSize());
        if (processed > 0) {
            log.debug("Dispatched {} records to receiver", processed);
        }
    }

    @Scheduled(fixedDelayString = "${app.scheduler.outbox-fixed-delay:5s}")
    public void dispatchOutbox() {
        int processed = requestDispatchProcessor.processBatch(coreProperties.getScheduler().getBatchSize());
        if (processed > 0) {
            log.debug("Dispatched {} outbox records to sender", processed);
        }
    }
}

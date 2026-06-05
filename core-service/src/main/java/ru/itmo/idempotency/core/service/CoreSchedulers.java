package ru.itmo.idempotency.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.core.config.CoreProperties;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreSchedulers implements SchedulingConfigurer {

    private final CoreProperties coreProperties;
    private final RequestDispatchProcessor requestDispatchProcessor;
    private final ReceiverDispatchProcessor receiverDispatchProcessor;
    private final ReplyTimeoutRecoveryService replyTimeoutRecoveryService;
    private final CleanupService cleanupService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(
            () -> requestDispatchProcessor.processBatch(coreProperties.getScheduler().getBatchSize()),
            ctx -> after(ctx, coreProperties.getScheduler().getOutboxFixedDelay())
        );
        registrar.addTriggerTask(
            () -> receiverDispatchProcessor.processBatch(coreProperties.getScheduler().getBatchSize()),
            ctx -> after(ctx, coreProperties.getScheduler().getDeliveryFixedDelay())
        );
        registrar.addTriggerTask(
            () -> replyTimeoutRecoveryService.processBatch(coreProperties.getScheduler().getBatchSize()),
            ctx -> after(ctx, coreProperties.getScheduler().getReplyTimeoutFixedDelay())
        );
        registrar.addTriggerTask(
            cleanupService::cleanupCommitted,
            ctx -> after(ctx, coreProperties.getScheduler().getCleanupFixedDelay())
        );
    }

    private static Instant after(TriggerContext ctx, Duration delay) {
        Instant last = ctx.lastCompletion();
        return (last != null ? last : Instant.now()).plus(delay);
    }
}

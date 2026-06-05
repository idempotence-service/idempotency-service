package ru.itmo.idempotency.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
    private final ThreadPoolTaskScheduler coreTaskScheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.setTaskScheduler(coreTaskScheduler);
        addWorkerTasks(
                registrar,
                coreProperties.getScheduler().getOutboxWorkers(),
                () -> requestDispatchProcessor.processBatch(coreProperties.getScheduler().getBatchSize()),
                coreProperties.getScheduler().getOutboxFixedDelay()
        );
        addWorkerTasks(
                registrar,
                coreProperties.getScheduler().getDeliveryWorkers(),
                () -> receiverDispatchProcessor.processBatch(coreProperties.getScheduler().getBatchSize()),
                coreProperties.getScheduler().getDeliveryFixedDelay()
        );
        addWorkerTasks(
                registrar,
                coreProperties.getScheduler().getReplyTimeoutWorkers(),
                () -> replyTimeoutRecoveryService.processBatch(coreProperties.getScheduler().getBatchSize()),
                coreProperties.getScheduler().getReplyTimeoutFixedDelay()
        );
        registrar.addTriggerTask(
                cleanupService::cleanupExpiredRecords,
                ctx -> after(ctx, coreProperties.getScheduler().getCleanupFixedDelay())
        );
    }

    private void addWorkerTasks(ScheduledTaskRegistrar registrar, int workerCount, Runnable task, Duration delay) {
        for (int index = 0; index < workerCount; index++) {
            registrar.addTriggerTask(task, ctx -> after(ctx, delay));
        }
    }

    private static Instant after(TriggerContext ctx, Duration delay) {
        Instant last = ctx.lastCompletion();
        return (last != null ? last : Instant.now()).plus(delay);
    }
}

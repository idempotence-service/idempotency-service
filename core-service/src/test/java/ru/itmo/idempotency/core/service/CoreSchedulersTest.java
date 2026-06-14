package ru.itmo.idempotency.core.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import ru.itmo.idempotency.core.config.CoreProperties;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreSchedulersTest {

    @Mock
    private CoreProperties coreProperties;
    @Mock
    private CoreProperties.Scheduler scheduler;
    @Mock
    private RequestDispatchProcessor requestDispatchProcessor;
    @Mock
    private ReceiverDispatchProcessor receiverDispatchProcessor;
    @Mock
    private ReplyTimeoutRecoveryService replyTimeoutRecoveryService;
    @Mock
    private CleanupService cleanupService;
    @Mock
    private ThreadPoolTaskScheduler coreTaskScheduler;

    private CoreSchedulers coreSchedulers;

    @BeforeEach
    void setUp() {
        when(coreProperties.getScheduler()).thenReturn(scheduler);
        when(scheduler.getOutboxWorkers()).thenReturn(2);
        when(scheduler.getDeliveryWorkers()).thenReturn(1);
        when(scheduler.getReplyTimeoutWorkers()).thenReturn(1);
        when(scheduler.getBatchSize()).thenReturn(50);
        when(scheduler.getOutboxFixedDelay()).thenReturn(Duration.ofSeconds(5));
        when(scheduler.getDeliveryFixedDelay()).thenReturn(Duration.ofSeconds(7));
        when(scheduler.getReplyTimeoutFixedDelay()).thenReturn(Duration.ofSeconds(9));
        when(scheduler.getCleanupFixedDelay()).thenReturn(Duration.ofSeconds(11));

        coreSchedulers = new CoreSchedulers(
                coreProperties,
                requestDispatchProcessor,
                receiverDispatchProcessor,
                replyTimeoutRecoveryService,
                cleanupService,
                coreTaskScheduler
        );
    }

    @Test
    void shouldRegisterExpectedWorkerTasks() {
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);

        coreSchedulers.configureTasks(registrar);

        verify(registrar).setTaskScheduler(coreTaskScheduler);
        verify(registrar, atLeastOnce()).addTriggerTask(any(Runnable.class), any(Trigger.class));
    }

    @Test
    void shouldScheduleNextExecutionAfterDelay() {
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);

        coreSchedulers.configureTasks(registrar);

        verify(registrar, atLeastOnce()).addTriggerTask(any(Runnable.class), triggerCaptor.capture());
        Trigger trigger = triggerCaptor.getAllValues().getFirst();
        TriggerContext context = mock(TriggerContext.class);
        when(context.lastCompletion()).thenReturn(Instant.parse("2024-01-01T00:00:00Z"));

        Instant nextExecution = trigger.nextExecution(context);

        Assertions.assertEquals(Instant.parse("2024-01-01T00:00:05Z"), nextExecution);
    }

    @Test
    void shouldUseNowWhenNoPreviousCompletionExists() {
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);

        coreSchedulers.configureTasks(registrar);

        verify(registrar, atLeastOnce()).addTriggerTask(any(Runnable.class), triggerCaptor.capture());
        Trigger trigger = triggerCaptor.getAllValues().getFirst();
        TriggerContext context = mock(TriggerContext.class);
        when(context.lastCompletion()).thenReturn(null);

        Instant before = Instant.now();
        Instant nextExecution = trigger.nextExecution(context);
        Instant after = Instant.now().plus(Duration.ofSeconds(5));

        Assertions.assertFalse(nextExecution.isBefore(before.plus(Duration.ofSeconds(5))));
        Assertions.assertFalse(nextExecution.isAfter(after));
    }
}

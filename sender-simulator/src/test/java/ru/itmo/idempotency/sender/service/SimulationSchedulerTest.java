package ru.itmo.idempotency.sender.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.sender.config.SenderProperties;

import java.lang.reflect.Method;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimulationSchedulerTest {

    @Mock
    private SenderDispatchService senderDispatchService;

    private SenderProperties senderProperties;
    private SimulationScheduler scheduler;

    @BeforeEach
    void setUp() {
        senderProperties = new SenderProperties();
        SenderProperties.Simulation simulation = senderProperties.getSimulation();
        simulation.setEnabled(true);
        simulation.setIntegration("system1-to-system2");
        simulation.setDuplicateEvery(2);
        simulation.setBurstSize(1);
        simulation.setInterval(Duration.ofMillis(1));
        simulation.setPause(Duration.ZERO);
        scheduler = new SimulationScheduler(senderProperties, senderDispatchService);
    }

    @Test
    void shouldSendSingleMessageOnNormalTick() throws Exception {
        senderProperties.getSimulation().setDuplicateEvery(3);
        when(senderDispatchService.send(any(), isNull(), isNull(), any(), eq(1))).thenReturn("uid-1");

        invokeSimulateTick();

        verify(senderDispatchService).send(eq("system1-to-system2"), isNull(), isNull(), any(), eq(1));
    }

    @Test
    void shouldSendDuplicateOnMatchingTick() throws Exception {
        when(senderDispatchService.send(any(), isNull(), isNull(), any(), eq(1))).thenReturn("uid-1");
        when(senderDispatchService.send(any(), isNull(), isNull(), any(), eq(2))).thenReturn("uid-dup");

        invokeSimulateTick();
        invokeSimulateTick();

        verify(senderDispatchService).send(eq("system1-to-system2"), isNull(), isNull(), any(), eq(2));
    }

    @Test
    void shouldSleepWhenSimulationDisabled() throws Exception {
        senderProperties.getSimulation().setEnabled(false);
        Thread virtualThread = Thread.ofVirtual().name("test-loop").start(() -> {
            try {
                Method loop = SimulationScheduler.class.getDeclaredMethod("loop");
                loop.setAccessible(true);
                loop.invoke(scheduler);
            } catch (Exception ignored) {
                // interrupted exit is expected
            }
        });
        Thread.sleep(100);
        virtualThread.interrupt();
        virtualThread.join(2_000);
    }

    private void invokeSimulateTick() throws Exception {
        Method method = SimulationScheduler.class.getDeclaredMethod("simulateTick");
        method.setAccessible(true);
        method.invoke(scheduler);
    }
}

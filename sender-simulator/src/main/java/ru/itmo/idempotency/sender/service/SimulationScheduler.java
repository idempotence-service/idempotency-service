package ru.itmo.idempotency.sender.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.sender.config.SenderProperties;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.simulation", name = "enabled", havingValue = "true")
public class SimulationScheduler implements ApplicationRunner {

    private final SenderProperties senderProperties;
    private final SenderDispatchService senderDispatchService;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().name("simulation-loop").start(this::loop);
    }

    private void loop() {
        SenderProperties.Simulation config = senderProperties.getSimulation();
        while (!Thread.currentThread().isInterrupted()) {
            for (int i = 0; i < config.getBurstSize(); i++) {
                if (Thread.currentThread().isInterrupted()) return;
                simulateTick();
                if (i < config.getBurstSize() - 1) {
                    sleep(config.getInterval());
                }
            }
            Duration pause = config.getPause();
            if (!pause.isZero()) {
                sleep(pause);
            }
        }
    }

    private void simulateTick() {
        SenderProperties.Simulation config = senderProperties.getSimulation();
        int tick = counter.incrementAndGet();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("orderId", tick);
        payload.put("amount", tick * 100);
        payload.put("comment", "simulation tick " + tick);

        boolean sendDuplicate = tick % config.getDuplicateEvery() == 0;
        int duplicates = sendDuplicate ? 2 : 1;

        String uid = senderDispatchService.send(config.getIntegration(), null, null, payload, duplicates);
        if (sendDuplicate) {
            log.info("[simulation] tick={} sent uid={} with duplicate", tick, uid);
        } else {
            log.info("[simulation] tick={} sent uid={}", tick, uid);
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

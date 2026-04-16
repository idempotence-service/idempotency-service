package ru.itmo.idempotency.sender.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.sender.config.SenderProperties;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.simulation", name = "enabled", havingValue = "true")
public class SimulationScheduler {

    private final SenderProperties senderProperties;
    private final SenderDispatchService senderDispatchService;

    private final AtomicInteger counter = new AtomicInteger(0);

    @Scheduled(fixedDelayString = "${app.simulation.interval:10s}")
    public void simulate() {
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
}

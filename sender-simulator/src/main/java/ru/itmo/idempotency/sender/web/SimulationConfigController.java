package ru.itmo.idempotency.sender.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.idempotency.common.web.ApiResponse;
import ru.itmo.idempotency.sender.config.SenderProperties;

import java.time.Duration;

@Validated
@RestController
@RequestMapping("/api/sender/config")
@RequiredArgsConstructor
public class SimulationConfigController {

    private final SenderProperties senderProperties;

    @GetMapping
    public ApiResponse<SimulationConfigDtos.SimulationConfigResponse> getConfig() {
        SenderProperties.Simulation sim = senderProperties.getSimulation();
        return ApiResponse.success(new SimulationConfigDtos.SimulationConfigResponse(
            sim.isEnabled(),
            sim.getIntegration(),
            sim.getInterval().getSeconds(),
            sim.getDuplicateEvery(),
            sim.getBurstSize(),
            sim.getPause().getSeconds()
        ));
    }

    @PutMapping
    public ApiResponse<String> updateConfig(@Valid @RequestBody SimulationConfigDtos.SimulationConfigRequest req) {
        SenderProperties.Simulation sim = senderProperties.getSimulation();
        if (req.enabled() != null) sim.setEnabled(req.enabled());
        if (req.integration() != null && !req.integration().isBlank()) sim.setIntegration(req.integration());
        if (req.intervalSeconds() != null) sim.setInterval(Duration.ofSeconds(req.intervalSeconds()));
        if (req.duplicateEvery() != null) sim.setDuplicateEvery(req.duplicateEvery());
        if (req.burstSize() != null) sim.setBurstSize(req.burstSize());
        if (req.pauseSeconds() != null) sim.setPause(Duration.ofSeconds(req.pauseSeconds()));
        return ApiResponse.success("ok");
    }
}

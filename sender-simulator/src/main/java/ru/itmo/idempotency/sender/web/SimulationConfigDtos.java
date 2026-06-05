package ru.itmo.idempotency.sender.web;

import jakarta.validation.constraints.Min;

public class SimulationConfigDtos {

    public record SimulationConfigResponse(
        boolean enabled,
        String integration,
        long intervalSeconds,
        int duplicateEvery,
        int burstSize,
        long pauseSeconds
    ) {}

    public record SimulationConfigRequest(
        Boolean enabled,
        String integration,
        @Min(1) Long intervalSeconds,
        @Min(1) Integer duplicateEvery,
        @Min(1) Integer burstSize,
        @Min(0) Long pauseSeconds
    ) {}
}

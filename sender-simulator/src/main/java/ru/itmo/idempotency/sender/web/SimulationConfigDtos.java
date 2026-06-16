package ru.itmo.idempotency.sender.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;

public class SimulationConfigDtos {

    public record SimulationConfigResponse(
        boolean enabled,
        String integration,
        double intervalSeconds,
        int duplicateEvery,
        int burstSize,
        double pauseSeconds
    ) {}

    public record SimulationConfigRequest(
        Boolean enabled,
        String integration,
        @DecimalMin("0.001") Double intervalSeconds,
        @Min(1) Integer duplicateEvery,
        @Min(1) Integer burstSize,
        @DecimalMin("0.0") Double pauseSeconds
    ) {}
}

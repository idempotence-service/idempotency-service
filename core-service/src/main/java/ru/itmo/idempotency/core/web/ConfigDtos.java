package ru.itmo.idempotency.core.web;

import jakarta.validation.constraints.Min;

public class ConfigDtos {

    public record SchedulerConfig(
        @Min(1) Long outboxFixedDelaySeconds,
        @Min(1) Long deliveryFixedDelaySeconds,
        @Min(1) Long replyTimeoutFixedDelaySeconds,
        @Min(1) Long cleanupFixedDelaySeconds,
        @Min(1) Integer batchSize
    ) {}

    public record ListenerConfig(
        @Min(1) Integer inboundConcurrency,
        @Min(1) Integer replyConcurrency
    ) {}

    public record ResilienceConfig(
        @Min(1) Long outboxRetryDelaySeconds,
        @Min(1) Long deliveryRetryDelaySeconds,
        @Min(1) Long replyTimeoutSeconds,
        @Min(1) Long leaseDurationSeconds,
        @Min(1) Integer maxAttempts
    ) {}

    public record CleanupConfig(
        @Min(1) Long retentionSeconds,
        @Min(1) Integer batchSize
    ) {}

    public record FullConfig(
        SchedulerConfig scheduler,
        ListenerConfig listener,
        ResilienceConfig resilience,
        CleanupConfig cleanup
    ) {}
}

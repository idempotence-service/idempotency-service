package ru.itmo.idempotency.core.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public class ConfigDtos {

    public record SchedulerConfig(
        @DecimalMin("0.001") Double outboxFixedDelaySeconds,
        @DecimalMin("0.001") Double deliveryFixedDelaySeconds,
        @DecimalMin("0.001") Double replyTimeoutFixedDelaySeconds,
        @DecimalMin("0.001") Double cleanupFixedDelaySeconds,
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

    public record IntegrationInfo(
            String integrationName,
            String serviceName,
            ChannelInfo inbound,
            ChannelInfo requestOut,
            ChannelInfo replyIn,
            ChannelInfo replyOut,
            boolean idempotencyEnabled
    ) {}


    public record ChannelInfo(
            String bootstrapServers,
            String topic,
            String group,
            Integer partitions,
            Short replicationFactor
    ) {}
}

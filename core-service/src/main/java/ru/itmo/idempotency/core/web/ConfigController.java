package ru.itmo.idempotency.core.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.web.ApiResponse;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.service.CoreMetrics;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final CoreProperties coreProperties;
    private final RouteCatalog routeCatalog;
    private final CoreMetrics coreMetrics;

    @GetMapping
    public ApiResponse<ConfigDtos.FullConfig> getConfig() {
        CoreProperties.Scheduler s = coreProperties.getScheduler();
        CoreProperties.Listener l = coreProperties.getListener();
        CoreProperties.Resilience r = coreProperties.getResilience();
        CoreProperties.Cleanup c = coreProperties.getCleanup();

        return ApiResponse.success(new ConfigDtos.FullConfig(
            new ConfigDtos.SchedulerConfig(
                s.getOutboxFixedDelay().getSeconds(),
                s.getDeliveryFixedDelay().getSeconds(),
                s.getReplyTimeoutFixedDelay().getSeconds(),
                s.getCleanupFixedDelay().getSeconds(),
                s.getBatchSize()
            ),
            new ConfigDtos.ListenerConfig(
                l.getInboundConcurrency(),
                l.getReplyConcurrency()
            ),
            new ConfigDtos.ResilienceConfig(
                r.getOutboxRetryDelay().getSeconds(),
                r.getDeliveryRetryDelay().getSeconds(),
                r.getReplyTimeout().getSeconds(),
                r.getLeaseDuration().getSeconds(),
                r.getMaxAttempts()
            ),
            new ConfigDtos.CleanupConfig(
                c.getRetention().getSeconds(),
                c.getBatchSize()
            )
        ));
    }

    @PutMapping("/scheduler")
    public ApiResponse<String> updateScheduler(@Valid @RequestBody ConfigDtos.SchedulerConfig req) {
        CoreProperties.Scheduler s = coreProperties.getScheduler();
        if (req.outboxFixedDelaySeconds() != null) s.setOutboxFixedDelay(Duration.ofSeconds(req.outboxFixedDelaySeconds()));
        if (req.deliveryFixedDelaySeconds() != null) s.setDeliveryFixedDelay(Duration.ofSeconds(req.deliveryFixedDelaySeconds()));
        if (req.replyTimeoutFixedDelaySeconds() != null) s.setReplyTimeoutFixedDelay(Duration.ofSeconds(req.replyTimeoutFixedDelaySeconds()));
        if (req.cleanupFixedDelaySeconds() != null) s.setCleanupFixedDelay(Duration.ofSeconds(req.cleanupFixedDelaySeconds()));
        if (req.batchSize() != null) s.setBatchSize(req.batchSize());
        coreMetrics.recordConfigUpdate("scheduler");
        log.info("Scheduler config updated: {}", req);
        return ApiResponse.success("ok");
    }

    @PutMapping("/resilience")
    public ApiResponse<String> updateResilience(@Valid @RequestBody ConfigDtos.ResilienceConfig req) {
        CoreProperties.Resilience r = coreProperties.getResilience();
        if (req.outboxRetryDelaySeconds() != null) r.setOutboxRetryDelay(Duration.ofSeconds(req.outboxRetryDelaySeconds()));
        if (req.deliveryRetryDelaySeconds() != null) r.setDeliveryRetryDelay(Duration.ofSeconds(req.deliveryRetryDelaySeconds()));
        if (req.replyTimeoutSeconds() != null) r.setReplyTimeout(Duration.ofSeconds(req.replyTimeoutSeconds()));
        if (req.leaseDurationSeconds() != null) r.setLeaseDuration(Duration.ofSeconds(req.leaseDurationSeconds()));
        if (req.maxAttempts() != null) r.setMaxAttempts(req.maxAttempts());
        coreMetrics.recordConfigUpdate("resilience");
        log.info("Resilience config updated: {}", req);
        return ApiResponse.success("ok");
    }

    @PutMapping("/cleanup")
    public ApiResponse<String> updateCleanup(@Valid @RequestBody ConfigDtos.CleanupConfig req) {
        CoreProperties.Cleanup c = coreProperties.getCleanup();
        if (req.retentionSeconds() != null) c.setRetention(Duration.ofSeconds(req.retentionSeconds()));
        if (req.batchSize() != null) c.setBatchSize(req.batchSize());
        coreMetrics.recordConfigUpdate("cleanup");
        log.info("Cleanup config updated: {}", req);
        return ApiResponse.success("ok");
    }

    @PutMapping("/listener")
    public ApiResponse<String> updateListener(@Valid @RequestBody ConfigDtos.ListenerConfig req) {
        CoreProperties.Listener l = coreProperties.getListener();
        if (req.inboundConcurrency() != null) l.setInboundConcurrency(req.inboundConcurrency());
        if (req.replyConcurrency() != null) l.setReplyConcurrency(req.replyConcurrency());
        coreMetrics.recordConfigUpdate("listener");
        log.info("Listener config updated: {}", req);
        return ApiResponse.success("ok");
    }

    @GetMapping("/integrations")
    public ApiResponse<List<ConfigDtos.IntegrationInfo>> getIntegrations() {
        List<ConfigDtos.IntegrationInfo> integrations = routeCatalog.getAllRoutes().stream()
                .map(this::toIntegrationInfo)
                .sorted(Comparator.comparing(ConfigDtos.IntegrationInfo::integrationName))
                .toList();

        return ApiResponse.success(integrations);
    }

    @GetMapping("/integrations/enabled")
    public ApiResponse<List<ConfigDtos.IntegrationInfo>> getEnabledIntegrations() {
        List<ConfigDtos.IntegrationInfo> integrations = routeCatalog.getEnabledRoutes().stream()
                .map(this::toIntegrationInfo)
                .sorted(Comparator.comparing(ConfigDtos.IntegrationInfo::integrationName))
                .toList();

        return ApiResponse.success(integrations);
    }

    private ConfigDtos.IntegrationInfo toIntegrationInfo(RouteModels.RouteSnapshot route) {
        return new ConfigDtos.IntegrationInfo(
                route.integration(),
                route.service(),
                toChannelInfo(route.inbound()),
                toChannelInfo(route.requestOut()),
                toChannelInfo(route.replyIn()),
                toChannelInfo(route.replyOut()),
                route.idempotencyEnabled()
        );
    }

    private ConfigDtos.ChannelInfo toChannelInfo(RouteModels.RouteChannel channel) {
        if (channel == null) {
            return null;
        }
        return new ConfigDtos.ChannelInfo(
                channel.bootstrapServers(),
                channel.topic(),
                channel.group(),
                channel.partitions(),
                channel.replicationFactor()
        );
    }

}

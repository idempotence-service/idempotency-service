package ru.itmo.idempotency.core.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class CoreProperties {

    @NotBlank
    private String routesFile = "config/routes.yaml";

    @Valid
    private Scheduler scheduler = new Scheduler();

    @Valid
    private Listener listener = new Listener();

    @Valid
    private Resilience resilience = new Resilience();

    @Valid
    private Cleanup cleanup = new Cleanup();

    @Valid
    private Security security = new Security();

    @Getter
    @Setter
    public static class Scheduler {
        private Duration outboxFixedDelay = Duration.ofSeconds(5);
        private Duration deliveryFixedDelay = Duration.ofSeconds(5);
        private Duration replyTimeoutFixedDelay = Duration.ofSeconds(30);
        private Duration cleanupFixedDelay = Duration.ofDays(1);
        @Min(1)
        private int batchSize = 100;
    }

    @Getter
    @Setter
    public static class Listener {
        @Min(1)
        private int inboundConcurrency = 1;
        @Min(1)
        private int replyConcurrency = 1;
    }

    @Getter
    @Setter
    public static class Resilience {
        private Duration outboxRetryDelay = Duration.ofSeconds(10);
        private Duration deliveryRetryDelay = Duration.ofSeconds(10);
        private Duration replyTimeout = Duration.ofMinutes(1);
        @Min(1)
        private int maxAttempts = 5;
    }

    @Getter
    @Setter
    public static class Cleanup {
        private Duration retention = Duration.ofDays(7);
        @Min(1)
        private int batchSize = 500;
    }

    @Getter
    @Setter
    public static class Security {
        @Valid
        private List<TokenConfig> tokens = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class TokenConfig {
        @NotBlank
        private String token;
        @NotBlank
        private String principal;
        private List<String> roles = new ArrayList<>();
    }
}

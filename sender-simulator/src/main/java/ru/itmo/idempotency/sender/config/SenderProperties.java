package ru.itmo.idempotency.sender.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class SenderProperties {

    @NotBlank
    private String routesFile = "config/routes.yaml";

    private Simulation simulation = new Simulation();

    @Getter
    @Setter
    public static class Simulation {
        private boolean enabled = false;
        private String integration = "system1-to-system2";
        private Duration interval = Duration.ofSeconds(10);
        private int duplicateEvery = 3;
    }
}

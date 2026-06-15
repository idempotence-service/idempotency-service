package ru.itmo.idempotency.sender.config;

import jakarta.validation.constraints.Min;
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

    private State state = new State();

    @Getter
    @Setter
    public static class Simulation {
        private boolean enabled = false;
        private String integration = "system1-to-system2";
        private Duration interval = Duration.ofSeconds(2);
        private int duplicateEvery = 2;
        private int burstSize = 5;
        private Duration pause = Duration.ofSeconds(5);
    }

    @Getter
    @Setter
    public static class State {
        private boolean storeHistory = true;
        @Min(1)
        private int historyLimit;
    }
}

package ru.itmo.idempotency.receiver.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app")
public class ReceiverProperties {

    @NotBlank
    private String routesFile = "config/routes.yaml";

    private State state = new State();

    @Getter
    @Setter
    public static class State {
        private boolean storeHistory = true;
        @Min(1)
        private int historyLimit;
    }
}

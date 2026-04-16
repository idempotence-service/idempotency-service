package ru.itmo.idempotency.sender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"ru.itmo.idempotency.sender", "ru.itmo.idempotency.common"})
@ConfigurationPropertiesScan
@EnableScheduling
public class SenderSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenderSimulatorApplication.class, args);
    }
}

package ru.itmo.idempotency.common.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class RoutesFileLoaderTest {

    private final RoutesFileLoader loader = new RoutesFileLoader();

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadValidRoutesFile() throws IOException {
        Path routesFile = tempDir.resolve("routes.yaml");
        Files.writeString(routesFile, """
                service:
                  name: sender-service
                kafka:
                  routes:
                    system1-to-system2:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: sender.events.inbound
                """);

        RouteModels.RoutesFile routes = loader.load(routesFile);

        Assertions.assertEquals("sender-service", routes.service().name());
        Assertions.assertTrue(routes.kafka().routes().containsKey("system1-to-system2"));
    }

    @Test
    void shouldFailWhenRoutesFileMissing() {
        Path missing = tempDir.resolve("missing.yaml");
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> loader.load(missing));
        Assertions.assertTrue(exception.getMessage().contains("Routes file not found"));
    }

    @Test
    void shouldFailWhenRoutesStructureInvalid() throws IOException {
        Path routesFile = tempDir.resolve("empty.yaml");
        Files.writeString(routesFile, "service:\n  name: sender-service\n");

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> loader.load(routesFile));
        Assertions.assertTrue(exception.getMessage().contains("empty or invalid"));
    }

    @Test
    void shouldFailWhenRoutesFileUnreadable() throws IOException {
        Path routesFile = tempDir.resolve("routes.yaml");
        Files.writeString(routesFile, "service:\n  name: [unclosed");

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> loader.load(routesFile));
        Assertions.assertTrue(exception.getMessage().contains("Unable to read routes file"));
    }
}

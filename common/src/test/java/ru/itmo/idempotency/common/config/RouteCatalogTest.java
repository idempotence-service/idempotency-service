package ru.itmo.idempotency.common.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class RouteCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseRoutesWithDefaultServiceName() throws IOException {
        Path routesFile = writeRoutes("""
                service:
                  name: sender-service
                kafka:
                  routes:
                    system1-to-system2:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: sender.events.inbound
                      receiver:
                        producer:
                          host: localhost:9092
                          topic: receiver.events.unique
                """);

        RouteCatalog catalog = new RouteCatalog(routesFile.toString(), new RoutesFileLoader());

        RouteModels.RouteSnapshot route = catalog.getRequiredRoute("system1-to-system2");
        Assertions.assertEquals("sender-service", route.service());
        Assertions.assertEquals("sender.events.inbound", route.inbound().topic());
        Assertions.assertTrue(route.idempotencyEnabled());
    }

    @Test
    void shouldUseRouteSpecificServiceName() throws IOException {
        Path routesFile = writeRoutes("""
                service:
                  name: default-service
                kafka:
                  routes:
                    custom-route:
                      service: custom-service
                      sender:
                        producer:
                          host: localhost:9092
                          topic: sender.events.inbound
                """);

        RouteCatalog catalog = new RouteCatalog(routesFile.toString(), new RoutesFileLoader());

        Assertions.assertEquals("custom-service", catalog.getRequiredRoute("custom-route").service());
    }

    @Test
    void shouldFilterDisabledIdempotencyRoutes() throws IOException {
        Path routesFile = writeRoutes("""
                service:
                  name: sender-service
                kafka:
                  routes:
                    enabled-route:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: sender.events.inbound
                      idempotency:
                        enabled: true
                    disabled-route:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: sender.events.inbound
                      idempotency:
                        enabled: false
                """);

        RouteCatalog catalog = new RouteCatalog(routesFile.toString(), new RoutesFileLoader());

        Assertions.assertEquals(2, catalog.getAllRoutes().size());
        Assertions.assertEquals(1, catalog.getEnabledRoutes().size());
        Assertions.assertEquals("enabled-route", catalog.getEnabledRoutes().iterator().next().integration());
    }

    @Test
    void shouldThrowWhenRouteMissing() throws IOException {
        RouteCatalog catalog = new RouteCatalog(writeRoutes(minimalRoutes()).toString(), new RoutesFileLoader());

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> catalog.getRequiredRoute("missing-route")
        );
        Assertions.assertTrue(exception.getMessage().contains("Route not found"));
    }

    @Test
    void shouldReloadRoutesFromDisk() throws IOException {
        Path routesFile = writeRoutes(minimalRoutes());
        RouteCatalog catalog = new RouteCatalog(routesFile.toString(), new RoutesFileLoader());
        Assertions.assertEquals(1, catalog.getAllRoutes().size());

        Files.writeString(routesFile, """
                service:
                  name: sender-service
                kafka:
                  routes:
                    route-a:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: topic-a
                    route-b:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: topic-b
                """);
        catalog.reload();

        Assertions.assertEquals(2, catalog.getAllRoutes().size());
        Assertions.assertNotNull(catalog.getRequiredRoute("route-b"));
    }

    private Path writeRoutes(String yaml) throws IOException {
        Path routesFile = tempDir.resolve("routes.yaml");
        Files.writeString(routesFile, yaml);
        return routesFile;
    }

    private static String minimalRoutes() {
        return """
                service:
                  name: sender-service
                kafka:
                  routes:
                    system1-to-system2:
                      sender:
                        producer:
                          host: localhost:9092
                          topic: sender.events.inbound
                """;
    }
}

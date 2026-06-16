package ru.itmo.idempotency.common.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RoutesTestSupport {

    private RoutesTestSupport() {
    }

    public static String createRoutesFile(String bootstrapServers) {
        return writeRoutesFile(bootstrapServers, false);
    }

    public static String createRoutesFileWithDisabledIntegration(String bootstrapServers) {
        return writeRoutesFile(bootstrapServers, true);
    }

    private static String writeRoutesFile(String bootstrapServers, boolean includeDisabledRoute) {
        try {
            Path file = Files.createTempFile("routes-test-", ".yaml");
            String disabledRoute = includeDisabledRoute
                    ? """
                        disabled-route:
                          sender:
                            producer:
                              host: %s
                              topic: sender.events.inbound
                            consumer:
                              host: %s
                              topic: sender.events.request-out
                              group: sender-disabled-replies
                          receiver:
                            producer:
                              host: %s
                              topic: receiver.events.unique
                            consumer:
                              host: %s
                              topic: receiver.events.reply
                              group: receiver-disabled-replies
                          idempotency:
                            enabled: false
                    """.formatted(bootstrapServers, bootstrapServers, bootstrapServers, bootstrapServers)
                    : "";
            Files.writeString(file, """
                    service:
                      name: sender-service

                    kafka:
                      routes:
                        system1-to-system2:
                          sender:
                            producer:
                              host: %s
                              topic: sender.events.inbound
                            consumer:
                              host: %s
                              topic: sender.events.request-out
                              group: sender-test-replies
                          receiver:
                            producer:
                              host: %s
                              topic: receiver.events.unique
                            consumer:
                              host: %s
                              topic: receiver.events.reply
                              group: core-test-replies
                          idempotency:
                            enabled: true
                    %s""".formatted(
                    bootstrapServers, bootstrapServers, bootstrapServers, bootstrapServers,
                    disabledRoute
            ));
            return file.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

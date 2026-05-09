package ru.itmo.idempotency.core.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.common.kafka.KafkaTopicProvisioner;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CoreKafkaInitializer {

    private final RouteCatalog routeCatalog;
    private final KafkaTopicProvisioner kafkaTopicProvisioner;

    @PostConstruct
    void initializeTopics() {
        List<ru.itmo.idempotency.common.config.RouteModels.RouteChannel> channels = new ArrayList<>();
        routeCatalog.getEnabledRoutes().forEach(route -> {
            channels.add(route.inbound());
            channels.add(route.requestOut());
            channels.add(route.replyIn());
            channels.add(route.replyOut());
        });
        kafkaTopicProvisioner.ensureTopics(channels);
    }
}

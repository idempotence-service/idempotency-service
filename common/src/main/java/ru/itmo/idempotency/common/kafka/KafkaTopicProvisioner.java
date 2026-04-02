package ru.itmo.idempotency.common.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.itmo.idempotency.common.config.RouteModels;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaTopicProvisioner {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicProvisioner.class);

    public void ensureTopics(Collection<RouteModels.RouteChannel> channels) {
        Map<String, Set<String>> topicsByBootstrapServers = new LinkedHashMap<>();
        for (RouteModels.RouteChannel channel : channels) {
            if (channel == null || isBlank(channel.bootstrapServers()) || isBlank(channel.topic())) {
                continue;
            }
            topicsByBootstrapServers
                    .computeIfAbsent(channel.bootstrapServers(), ignored -> new LinkedHashSet<>())
                    .add(channel.topic());
        }

        topicsByBootstrapServers.forEach(this::ensureTopics);
    }

    private void ensureTopics(String bootstrapServers, Set<String> topics) {
        if (topics.isEmpty()) {
            return;
        }

        Map<String, Object> adminProperties = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
        );

        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            List<NewTopic> newTopics = topics.stream()
                    .filter(Objects::nonNull)
                    .map(topic -> new NewTopic(topic, 1, (short) 1))
                    .toList();

            var createTopicsResult = adminClient.createTopics(newTopics);
            for (Map.Entry<String, org.apache.kafka.common.KafkaFuture<Void>> entry : createTopicsResult.values().entrySet()) {
                try {
                    entry.getValue().get();
                    log.info("Created Kafka topic '{}' on '{}'", entry.getKey(), bootstrapServers);
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof TopicExistsException) {
                        log.debug("Kafka topic '{}' already exists on '{}'", entry.getKey(), bootstrapServers);
                        continue;
                    }
                    throw new IllegalStateException(
                            "Failed to create Kafka topic '%s' on '%s'".formatted(entry.getKey(), bootstrapServers),
                            ex.getCause()
                    );
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Interrupted while creating Kafka topic '%s' on '%s'".formatted(entry.getKey(), bootstrapServers),
                            ex
                    );
                }
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package ru.itmo.idempotency.sender.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.common.kafka.KafkaTopicProvisioner;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderKafkaInitializerTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private KafkaTopicProvisioner kafkaTopicProvisioner;

    @InjectMocks
    private SenderKafkaInitializer senderKafkaInitializer;

    @Test
    void shouldProvisionInboundAndRequestOutChannels() {
        RouteModels.RouteChannel inbound = channel("inbound");
        RouteModels.RouteChannel requestOut = channel("request-out");
        RouteModels.RouteSnapshot route = new RouteModels.RouteSnapshot("svc", "int", inbound, requestOut, null, null, true);
        when(routeCatalog.getAllRoutes()).thenReturn(List.of(route));

        senderKafkaInitializer.initializeTopics();

        verify(kafkaTopicProvisioner).ensureTopics(List.of(inbound, requestOut));
    }

    private static RouteModels.RouteChannel channel(String topic) {
        return new RouteModels.RouteChannel("localhost:9092", topic, "g", 1, (short) 1);
    }
}

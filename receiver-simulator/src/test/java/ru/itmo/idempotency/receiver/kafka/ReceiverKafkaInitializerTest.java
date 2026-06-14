package ru.itmo.idempotency.receiver.kafka;

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
class ReceiverKafkaInitializerTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private KafkaTopicProvisioner kafkaTopicProvisioner;

    @InjectMocks
    private ReceiverKafkaInitializer receiverKafkaInitializer;

    @Test
    void shouldProvisionReplyInAndReplyOutChannels() {
        RouteModels.RouteChannel replyIn = channel("reply-in");
        RouteModels.RouteChannel replyOut = channel("reply-out");
        RouteModels.RouteSnapshot route = new RouteModels.RouteSnapshot("svc", "int", null, null, replyIn, replyOut, true);
        when(routeCatalog.getAllRoutes()).thenReturn(List.of(route));

        receiverKafkaInitializer.initializeTopics();

        verify(kafkaTopicProvisioner).ensureTopics(List.of(replyIn, replyOut));
    }

    private static RouteModels.RouteChannel channel(String topic) {
        return new RouteModels.RouteChannel("localhost:9092", topic, "g", 1, (short) 1);
    }
}

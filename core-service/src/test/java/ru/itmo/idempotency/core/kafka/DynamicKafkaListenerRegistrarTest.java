package ru.itmo.idempotency.core.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.core.config.CoreProperties;
import ru.itmo.idempotency.core.service.AsyncReplyProcessor;
import ru.itmo.idempotency.core.service.InboundMessageProcessor;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicKafkaListenerRegistrarTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private InboundMessageProcessor inboundMessageProcessor;
    @Mock
    private AsyncReplyProcessor asyncReplyProcessor;

    private CoreProperties coreProperties;
    private DynamicKafkaListenerRegistrar registrar;

    @AfterEach
    void tearDown() {
        if (registrar != null && registrar.isRunning()) {
            registrar.stop();
        }
    }

    @Test
    void shouldStartAndStopIdempotently() {
        registrar = newRegistrar(routeWithReplyOut(), routeWithoutReplyOut());

        registrar.start();
        Assertions.assertTrue(registrar.isRunning());
        registrar.start();
        Assertions.assertTrue(registrar.isRunning());

        registrar.stop();
        Assertions.assertFalse(registrar.isRunning());
        registrar.stop();
        Assertions.assertFalse(registrar.isRunning());
    }

    @Test
    void shouldRegisterReplyListenerOnlyWhenReplyOutConfigured() {
        registrar = newRegistrar(routeWithReplyOut(), routeWithoutReplyOut());

        registrar.start();

        Assertions.assertEquals(Integer.MAX_VALUE, registrar.getPhase());
        registrar.stop();
    }

    private DynamicKafkaListenerRegistrar newRegistrar(RouteModels.RouteSnapshot... routes) {
        coreProperties = new CoreProperties();
        when(routeCatalog.getEnabledRoutes()).thenReturn(List.of(routes));
        return new DynamicKafkaListenerRegistrar(routeCatalog, inboundMessageProcessor, asyncReplyProcessor, coreProperties);
    }

    private static RouteModels.RouteSnapshot routeWithReplyOut() {
        RouteModels.RouteChannel inbound = channel("inbound-a");
        RouteModels.RouteChannel replyOut = channel("reply-out-a");
        return new RouteModels.RouteSnapshot("svc", "route-a", inbound, null, null, replyOut, true);
    }

    private static RouteModels.RouteSnapshot routeWithoutReplyOut() {
        RouteModels.RouteChannel inbound = channel("inbound-b");
        return new RouteModels.RouteSnapshot("svc", "route-b", inbound, null, null, null, true);
    }

    private static RouteModels.RouteChannel channel(String topic) {
        return new RouteModels.RouteChannel("127.0.0.1:19092", topic, null, 1, (short) 1);
    }
}

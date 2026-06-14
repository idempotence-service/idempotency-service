package ru.itmo.idempotency.sender.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.idempotency.common.config.RouteCatalog;
import ru.itmo.idempotency.common.config.RouteModels;
import ru.itmo.idempotency.sender.service.SenderStateService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SenderReplyListenerRegistrarTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private SenderStateService senderStateService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SenderReplyListenerRegistrar registrar;

    @BeforeEach
    void setUp() {
        lenient().when(routeCatalog.getAllRoutes()).thenReturn(List.of(route()));
        registrar = new SenderReplyListenerRegistrar(routeCatalog, senderStateService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        if (registrar != null && registrar.isRunning()) {
            registrar.stop();
        }
    }

    @Test
    void shouldStartAndStopIdempotently() {
        registrar.start();
        Assertions.assertTrue(registrar.isRunning());
        registrar.start();
        registrar.stop();
        Assertions.assertFalse(registrar.isRunning());
        registrar.stop();
    }

    @Test
    void shouldProcessValidReply() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "headers", Map.of("globalKey", "gk-1"),
                "payload", Map.of("result", "SUCCESS")
        ));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "gk-1", json);

        invokeProcessReply(route(), record);

        verify(senderStateService).recordReply(eq("system1-to-system2"), eq("gk-1"), any(), any());
    }

    @Test
    void shouldPassNullGlobalKeyWhenHeaderMissing() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "headers", Map.of(),
                "payload", Map.of("result", "SUCCESS")
        ));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, null, json);

        invokeProcessReply(route(), record);

        verify(senderStateService).recordReply(eq("system1-to-system2"), isNull(), any(), any());
    }

    @Test
    void shouldThrowOnInvalidJson() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "k", "not-json");

        InvocationTargetException exception = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeProcessReply(route(), record)
        );

        Assertions.assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    private static RouteModels.RouteSnapshot route() {
        RouteModels.RouteChannel requestOut = new RouteModels.RouteChannel("127.0.0.1:19092", "request-out", "g", 1, (short) 1);
        return new RouteModels.RouteSnapshot("svc", "system1-to-system2", null, requestOut, null, null, true);
    }

    private void invokeProcessReply(RouteModels.RouteSnapshot route, ConsumerRecord<String, String> record) throws Exception {
        Method method = SenderReplyListenerRegistrar.class.getDeclaredMethod("processReply", RouteModels.RouteSnapshot.class, ConsumerRecord.class);
        method.setAccessible(true);
        method.invoke(registrar, route, record);
    }
}

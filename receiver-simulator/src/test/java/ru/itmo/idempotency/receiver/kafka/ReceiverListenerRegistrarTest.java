package ru.itmo.idempotency.receiver.kafka;

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
import ru.itmo.idempotency.receiver.service.ReceiverMode;
import ru.itmo.idempotency.receiver.service.ReceiverReplyService;
import ru.itmo.idempotency.receiver.service.ReceiverStateService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiverListenerRegistrarTest {

    @Mock
    private RouteCatalog routeCatalog;
    @Mock
    private ReceiverStateService receiverStateService;
    @Mock
    private ReceiverReplyService receiverReplyService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReceiverListenerRegistrar registrar;

    @BeforeEach
    void setUp() {
        lenient().when(routeCatalog.getAllRoutes()).thenReturn(List.of(route()));
        registrar = new ReceiverListenerRegistrar(routeCatalog, receiverStateService, receiverReplyService, objectMapper);
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
        registrar.stop();
        registrar.stop();
        Assertions.assertFalse(registrar.isRunning());
    }

    @Test
    void shouldUseRecordKeyWhenGlobalKeyHeaderMissing() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "headers", Map.of("uid", "u-1"),
                "payload", Map.of("x", 1)
        ));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "record-key", json);
        ReceiverStateService.ReceivedMessage message = message("record-key");
        when(receiverStateService.record(eq("system1-to-system2"), eq("record-key"), any(), any())).thenReturn(message);
        when(receiverStateService.modeFor("system1-to-system2")).thenReturn(ReceiverMode.MANUAL);

        invokeProcessIncoming(route(), record);

        verify(receiverStateService).record(eq("system1-to-system2"), eq("record-key"), any(), any());
        verify(receiverReplyService, never()).sendReply(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void shouldAutoReplyOnAutoSuccessMode() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "headers", Map.of("globalKey", "gk-1"),
                "payload", Map.of("x", 1)
        ));
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "gk-1", json);
        ReceiverStateService.ReceivedMessage message = message("gk-1");
        when(receiverStateService.record(anyString(), anyString(), any(), any())).thenReturn(message);
        when(receiverStateService.modeFor("system1-to-system2")).thenReturn(ReceiverMode.AUTO_SUCCESS);

        invokeProcessIncoming(route(), record);

        verify(receiverReplyService).sendReply("system1-to-system2", "gk-1", "SUCCESS", false, "Обработка завершена успешно");
    }

    @Test
    void shouldAutoReplyFailResend() throws Exception {
        ReceiverStateService.ReceivedMessage message = message("gk-1");
        when(receiverStateService.modeFor("system1-to-system2")).thenReturn(ReceiverMode.AUTO_FAIL_RESEND);

        invokeProcessAutoReply(message);

        verify(receiverReplyService).sendReply("system1-to-system2", "gk-1", "FAIL", true, "Нужна повторная отправка");
    }

    @Test
    void shouldAutoReplyFailNoResend() throws Exception {
        ReceiverStateService.ReceivedMessage message = message("gk-1");
        when(receiverStateService.modeFor("system1-to-system2")).thenReturn(ReceiverMode.AUTO_FAIL_NO_RESEND);

        invokeProcessAutoReply(message);

        verify(receiverReplyService).sendReply("system1-to-system2", "gk-1", "FAIL", false, "Обработка завершилась ошибкой");
    }

    @Test
    void shouldThrowOnInvalidJson() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "k", "{");

        InvocationTargetException exception = Assertions.assertThrows(
                InvocationTargetException.class,
                () -> invokeProcessIncoming(route(), record)
        );

        Assertions.assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    private static RouteModels.RouteSnapshot route() {
        RouteModels.RouteChannel replyIn = new RouteModels.RouteChannel("127.0.0.1:19092", "reply-in", "g", 1, (short) 1);
        return new RouteModels.RouteSnapshot("svc", "system1-to-system2", null, null, replyIn, null, true);
    }

    private static ReceiverStateService.ReceivedMessage message(String globalKey) {
        return new ReceiverStateService.ReceivedMessage(
                "system1-to-system2",
                globalKey,
                false,
                Map.of(),
                new ObjectMapper().createObjectNode(),
                OffsetDateTime.now()
        );
    }

    private void invokeProcessIncoming(RouteModels.RouteSnapshot route, ConsumerRecord<String, String> record) throws Exception {
        Method method = ReceiverListenerRegistrar.class.getDeclaredMethod("processIncoming", RouteModels.RouteSnapshot.class, ConsumerRecord.class);
        method.setAccessible(true);
        method.invoke(registrar, route, record);
    }

    private void invokeProcessAutoReply(ReceiverStateService.ReceivedMessage message) throws Exception {
        Method method = ReceiverListenerRegistrar.class.getDeclaredMethod("processAutoReply", ReceiverStateService.ReceivedMessage.class);
        method.setAccessible(true);
        method.invoke(registrar, message);
    }
}

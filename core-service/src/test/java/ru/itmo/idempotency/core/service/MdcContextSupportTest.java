package ru.itmo.idempotency.core.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcContextSupportTest {

    private final MdcContextSupport mdcContextSupport = new MdcContextSupport();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldPutContextValuesAndRestorePreviousState() {
        MDC.put("existing", "value");

        try (MdcContextSupport.Scope ignored = mdcContextSupport.open("gk-1", "uid-1", "integration-1", "owner-1")) {
            Assertions.assertEquals("gk-1", MDC.get("globalKey"));
            Assertions.assertEquals("uid-1", MDC.get("uid"));
            Assertions.assertEquals("integration-1", MDC.get("integration"));
            Assertions.assertEquals("owner-1", MDC.get("ownerId"));
        }

        Assertions.assertEquals("value", MDC.get("existing"));
        Assertions.assertNull(MDC.get("globalKey"));
    }

    @Test
    void shouldClearMdcWhenNoPreviousContextExisted() {
        try (MdcContextSupport.Scope ignored = mdcContextSupport.open("gk-1", null, "integration-1", null)) {
            Assertions.assertEquals("gk-1", MDC.get("globalKey"));
            Assertions.assertEquals("integration-1", MDC.get("integration"));
            Assertions.assertNull(MDC.get("uid"));
        }

        Assertions.assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty());
    }
}

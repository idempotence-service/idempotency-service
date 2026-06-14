package ru.itmo.idempotency.core.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DescriptionUtilsTest {

    @Test
    void shouldReturnNullForNullInput() {
        Assertions.assertNull(DescriptionUtils.limit(null));
    }

    @Test
    void shouldReturnShortValueUnchanged() {
        Assertions.assertEquals("short", DescriptionUtils.limit("short"));
    }

    @Test
    void shouldReturnExact255CharactersUnchanged() {
        String value = "x".repeat(255);
        Assertions.assertEquals(255, DescriptionUtils.limit(value).length());
        Assertions.assertEquals(value, DescriptionUtils.limit(value));
    }

    @Test
    void shouldTruncateLongValueWithEllipsis() {
        String value = "y".repeat(300);
        String limited = DescriptionUtils.limit(value);

        Assertions.assertEquals(255, limited.length());
        Assertions.assertTrue(limited.endsWith("..."));
        Assertions.assertEquals("y".repeat(252) + "...", limited);
    }
}

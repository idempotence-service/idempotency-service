package ru.itmo.idempotency.common.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void shouldCreateEmptyError() {
        ApiError error = ApiError.empty();

        Assertions.assertNull(error.errorUid());
        Assertions.assertNull(error.title());
        Assertions.assertNull(error.code());
        Assertions.assertNull(error.text());
    }

    @Test
    void shouldCreatePopulatedError() {
        ApiError error = new ApiError("uid-1", "title", "CODE", "details");

        Assertions.assertEquals("uid-1", error.errorUid());
        Assertions.assertEquals("title", error.title());
        Assertions.assertEquals("CODE", error.code());
        Assertions.assertEquals("details", error.text());
    }
}

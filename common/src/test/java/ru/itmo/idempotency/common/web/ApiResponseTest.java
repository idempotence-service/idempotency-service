package ru.itmo.idempotency.common.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void shouldBuildSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("payload");

        Assertions.assertTrue(response.success());
        Assertions.assertEquals("payload", response.data());
        Assertions.assertEquals(ApiError.empty(), response.error());
    }

    @Test
    void shouldBuildFailureResponseWithErrorUid() {
        ApiResponse<Void> response = ApiResponse.failure("Title", "CODE", "Details");

        Assertions.assertFalse(response.success());
        Assertions.assertNull(response.data());
        Assertions.assertEquals("Title", response.error().title());
        Assertions.assertEquals("CODE", response.error().code());
        Assertions.assertEquals("Details", response.error().text());
        Assertions.assertNotNull(response.error().errorUid());
        Assertions.assertFalse(response.error().errorUid().isBlank());
    }
}

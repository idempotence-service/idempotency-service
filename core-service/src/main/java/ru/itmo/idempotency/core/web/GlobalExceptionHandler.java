package ru.itmo.idempotency.core.web;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.itmo.idempotency.common.web.ApiResponse;
import ru.itmo.idempotency.core.service.CoreMetrics;
import ru.itmo.idempotency.core.service.EventNotFoundException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final CoreMetrics coreMetrics;

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        coreMetrics.recordApiError("validation");
        log.warn("Validation error while handling API request: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Ошибка валидации", "Некорректный входящий запрос", exception.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EventNotFoundException exception) {
        coreMetrics.recordApiError("event_not_found");
        log.warn("Requested entity was not found: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Техническая ошибка", "TECHNICAL_ERROR_02", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleInternalError(Exception exception) {
        coreMetrics.recordApiError("internal");
        log.error("Unhandled internal error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Техническая ошибка", "TECHNICAL_ERROR_99", exception.getMessage()));
    }
}

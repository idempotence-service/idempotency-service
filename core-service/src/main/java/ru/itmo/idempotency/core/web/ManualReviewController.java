package ru.itmo.idempotency.core.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.idempotency.common.web.ApiResponse;
import ru.itmo.idempotency.core.service.ManualReviewService;

import java.time.OffsetDateTime;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
public class ManualReviewController {

    private final ManualReviewService manualReviewService;

    @GetMapping("/get-error-events")
    public ApiResponse<Page<ManualReviewDtos.ErrorEventItem>> getErrorEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "asc") @NotBlank String sort
    ) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(sort)
                .orElseThrow(() -> new IllegalArgumentException("sort must be asc or desc"));
        return ApiResponse.success(manualReviewService.getErrorEvents(page, limit, direction));
    }

    @PostMapping("/restart-event")
    public ApiResponse<String> restartEvent(@Valid @RequestBody ManualReviewDtos.RestartEventRequest request) {
        return ApiResponse.success(manualReviewService.restart(request.globalKey()));
    }

    @GetMapping("/get-event-by-id")
    public ApiResponse<ManualReviewDtos.EventDetails> getEventById(@RequestParam @NotBlank String globalKey) {
        return ApiResponse.success(manualReviewService.getByGlobalKey(globalKey));
    }

    @GetMapping("/get-duplicate-events")
    public ApiResponse<Page<ManualReviewDtos.DuplicateEventItem>> getDuplicateEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.success(manualReviewService.getDuplicateEvents(page, limit));
    }

    @GetMapping("/get-duplicate-count")
    public ApiResponse<Long> getDuplicateCount() {
        return ApiResponse.success(manualReviewService.getDuplicateCount());
    }

    @GetMapping("/get-timeout-count")
    public ApiResponse<Long> getTimeoutCount() {
        return ApiResponse.success(manualReviewService.getTimeoutCount());
    }

    @GetMapping("/get-audit-activity")
    public ApiResponse<Map<String, Long>> getAuditActivity(
            @RequestParam(required = false) String since
    ) {
        if (since == null || since.isEmpty()) {
            return ApiResponse.success(manualReviewService.getAuditActivitySince(null));
        }
        OffsetDateTime sinceTime = OffsetDateTime.parse(since);
        return ApiResponse.success(manualReviewService.getAuditActivitySince(sinceTime));
    }
}

package ru.itmo.idempotency.core.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.idempotency.common.web.ApiResponse;
import ru.itmo.idempotency.core.service.CoreMetrics;
import ru.itmo.idempotency.core.service.ManualReviewService;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class ManualReviewController {

    private final ManualReviewService manualReviewService;
    private final CoreMetrics coreMetrics;

    @GetMapping("/get-error-events")
    public ApiResponse<Page<ManualReviewDtos.ErrorEventItem>> getErrorEvents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "asc") @NotBlank String sort
    ) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(sort)
                .orElseThrow(() -> new IllegalArgumentException("sort must be asc or desc"));
        Page<ManualReviewDtos.ErrorEventItem> result = manualReviewService.getErrorEvents(page, limit, direction);
        coreMetrics.recordManualReviewAction("get_error_events", "success");
        return ApiResponse.success(result);
    }

    @PostMapping("/restart-event")
    public ApiResponse<String> restartEvent(@Valid @RequestBody ManualReviewDtos.RestartEventRequest request) {
        log.info("Manual restart requested for {}", request.globalKey());
        String result = manualReviewService.restart(request.globalKey());
        coreMetrics.recordManualReviewAction("restart_event", "success");
        return ApiResponse.success(result);
    }

    @GetMapping("/get-event-by-id")
    public ApiResponse<ManualReviewDtos.EventDetails> getEventById(@RequestParam @NotBlank String globalKey) {
        ManualReviewDtos.EventDetails result = manualReviewService.getByGlobalKey(globalKey);
        coreMetrics.recordManualReviewAction("get_event_by_id", "success");
        return ApiResponse.success(result);
    }

    @GetMapping("/get-duplicate-events")
    public ApiResponse<List<ManualReviewDtos.DuplicateEventItem>> getDuplicateEvents() {
        List<ManualReviewDtos.DuplicateEventItem> result = manualReviewService.getDuplicateEvents();
        coreMetrics.recordManualReviewAction("get_duplicate_events", "success");
        return ApiResponse.success(result);
    }

    @GetMapping("/get-duplicate-count")
    public ApiResponse<Long> getDuplicateCount() {
        long result = manualReviewService.getDuplicateCount();
        coreMetrics.recordManualReviewAction("get_duplicate_count", "success");
        return ApiResponse.success(result);
    }

    @GetMapping("/get-timeout-count")
    public ApiResponse<Long> getTimeoutCount() {
        long result = manualReviewService.getTimeoutCount();
        coreMetrics.recordManualReviewAction("get_timeout_count", "success");
        return ApiResponse.success(result);
    }
}

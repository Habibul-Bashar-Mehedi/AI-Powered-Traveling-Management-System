package aptms.api;

import aptms.dto.vendor.AnalyticsSummaryDTO;
import aptms.security.SecurityUtils;
import aptms.services.VendorAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for vendor analytics and performance reporting.
 * Requirements: BRD §4.5 — Analytics & Reporting
 */
@RestController
@RequestMapping("/api/v1/vendor/analytics")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
@Tag(name = "Vendor Analytics", description = "Revenue trends, booking metrics and performance reports")
public class VendorAnalyticsController {

    private final VendorAnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Get analytics summary for configurable period (FR-ANL-001)")
    public ResponseEntity<AnalyticsSummaryDTO> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getSummary(userId, from, to));
    }

    private UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}


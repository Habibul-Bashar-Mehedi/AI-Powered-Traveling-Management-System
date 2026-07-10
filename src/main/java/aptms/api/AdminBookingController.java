package aptms.api;

import aptms.dto.vendor.UserBookingStatusSummaryDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.enums.VendorBookingStatus;
import aptms.services.AdminBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only admin visibility into every booking across all vendors and travelers,
 * covering both catalog bookings and "Book Instantly" quick-action requests
 * (both are persisted as VendorBooking rows).
 */
@RestController
@RequestMapping("/api/v1/admin/bookings")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Bookings", description = "View all bookings placed across the platform")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    @Operation(summary = "Get all bookings, optionally filtered by status")
    public ResponseEntity<List<VendorBookingDTO>> getAllBookings(
            @RequestParam(required = false) VendorBookingStatus status) {
        return ResponseEntity.ok(adminBookingService.getAllBookings(status));
    }

    @GetMapping("/status-summary")
    @Operation(summary = "Get booking counts grouped by status, across the whole platform")
    public ResponseEntity<UserBookingStatusSummaryDTO> getBookingStatusSummary() {
        return ResponseEntity.ok(adminBookingService.getBookingStatusSummary());
    }
}

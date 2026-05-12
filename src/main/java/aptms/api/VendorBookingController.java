package aptms.api;

import aptms.dto.vendor.VendorBookingDTO;
import aptms.enums.VendorBookingStatus;
import aptms.services.VendorBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for vendor booking inbox and lifecycle management.
 * Requirements: BRD §4.3 — Booking Control
 */
@RestController
@RequestMapping("/api/v1/vendor/bookings")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
@Tag(name = "Vendor Bookings", description = "Booking inbox and confirmation/rejection management")
public class VendorBookingController {

    private final VendorBookingService bookingService;

    @GetMapping
    @Operation(summary = "Get booking inbox with optional status filter (FR-BKG-001)")
    public ResponseEntity<List<VendorBookingDTO>> getBookings(
            @RequestParam(required = false) VendorBookingStatus status) {
        return ResponseEntity.ok(bookingService.getBookings(getCurrentUserId(), status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking detail (FR-BKG-004)")
    public ResponseEntity<VendorBookingDTO> getBookingDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.getBookingDetail(getCurrentUserId(), id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a pending booking (FR-BKG-002, FR-BKG-003)")
    public ResponseEntity<VendorBookingDTO> confirmBooking(@PathVariable UUID id) {
        return ResponseEntity.ok(bookingService.confirmBooking(getCurrentUserId(), id));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending booking")
    public ResponseEntity<VendorBookingDTO> rejectBooking(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(bookingService.rejectBooking(getCurrentUserId(), id, body.get("reason")));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a confirmed booking (FR-BKG-005)")
    public ResponseEntity<VendorBookingDTO> cancelBooking(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(bookingService.cancelBooking(getCurrentUserId(), id, body.get("reason")));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}


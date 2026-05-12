package aptms.api;

import aptms.dto.vendor.AdminVendorActionDTO;
import aptms.dto.vendor.PayoutRequestDTO;
import aptms.dto.vendor.VendorProfileDTO;
import aptms.services.AdminVendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for admin vendor management operations.
 * Requirements: BRD FR-REG-004, FR-REG-005 — Admin Review Queue & Approval/Rejection
 */
@RestController
@RequestMapping("/api/v1/admin/vendors")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Vendor Management", description = "Approve, reject, suspend vendors and manage payouts")
public class AdminVendorController {

    private final AdminVendorService adminVendorService;

    @GetMapping
    @Operation(summary = "Get all vendors")
    public ResponseEntity<List<VendorProfileDTO>> getAllVendors() {
        return ResponseEntity.ok(adminVendorService.getAllVendors());
    }

    @GetMapping("/pending")
    @Operation(summary = "Get vendors awaiting approval (FR-REG-004)")
    public ResponseEntity<List<VendorProfileDTO>> getPendingVendors() {
        return ResponseEntity.ok(adminVendorService.getPendingVendors());
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a vendor registration (FR-REG-005)")
    public ResponseEntity<VendorProfileDTO> approveVendor(@PathVariable UUID id) {
        UUID adminId = getCurrentUserId();
        return ResponseEntity.ok(adminVendorService.approveVendor(id, adminId));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a vendor registration (FR-REG-005)")
    public ResponseEntity<VendorProfileDTO> rejectVendor(
            @PathVariable UUID id,
            @RequestBody AdminVendorActionDTO action) {
        UUID adminId = getCurrentUserId();
        return ResponseEntity.ok(adminVendorService.rejectVendor(id, adminId, action.getReason()));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend an approved vendor account")
    public ResponseEntity<VendorProfileDTO> suspendVendor(
            @PathVariable UUID id,
            @RequestBody AdminVendorActionDTO action) {
        UUID adminId = getCurrentUserId();
        return ResponseEntity.ok(adminVendorService.suspendVendor(id, adminId, action.getReason()));
    }

    @PostMapping("/{id}/reinstate")
    @Operation(summary = "Reinstate a suspended vendor account")
    public ResponseEntity<VendorProfileDTO> reinstateVendor(@PathVariable UUID id) {
        UUID adminId = getCurrentUserId();
        return ResponseEntity.ok(adminVendorService.reinstateVendor(id, adminId));
    }

    @GetMapping("/payouts/pending")
    @Operation(summary = "Get pending payout requests")
    public ResponseEntity<List<PayoutRequestDTO>> getPendingPayouts() {
        return ResponseEntity.ok(adminVendorService.getPendingPayouts());
    }

    @PostMapping("/payouts/{payoutId}/process")
    @Operation(summary = "Approve or deny a payout request (FR-WAL-003)")
    public ResponseEntity<PayoutRequestDTO> processPayout(
            @PathVariable UUID payoutId,
            @RequestParam boolean approve,
            @RequestBody(required = false) AdminVendorActionDTO action) {
        UUID adminId = getCurrentUserId();
        String note = action != null ? action.getReason() : null;
        return ResponseEntity.ok(adminVendorService.processePayout(payoutId, adminId, approve, note));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}


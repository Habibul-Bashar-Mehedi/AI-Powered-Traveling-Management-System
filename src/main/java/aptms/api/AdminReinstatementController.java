package aptms.api;

import aptms.dto.vendor.ReinstatementRequestDTO;
import aptms.dto.vendor.ReviewReinstatementRequestDTO;
import aptms.security.SecurityUtils;
import aptms.services.ReinstatementRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin review of vendor reinstatement requests.
 */
@RestController
@RequestMapping("/api/v1/admin/reinstatement-requests")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Vendor Reinstatement", description = "Review suspended vendors' reinstatement requests")
public class AdminReinstatementController {

    private final ReinstatementRequestService reinstatementRequestService;

    @GetMapping
    @Operation(summary = "Get all reinstatement requests, optionally filtered by status")
    public ResponseEntity<List<ReinstatementRequestDTO>> getAllRequests(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(reinstatementRequestService.getAllRequests(status));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Approve or reject a reinstatement request")
    public ResponseEntity<ReinstatementRequestDTO> reviewRequest(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewReinstatementRequestDTO body) {
        boolean approve = "APPROVE".equalsIgnoreCase(body.getDecision());
        if (!approve && !"REJECT".equalsIgnoreCase(body.getDecision())) {
            throw new IllegalArgumentException("decision must be APPROVE or REJECT");
        }
        UUID adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                reinstatementRequestService.reviewRequest(id, adminId, approve, body.getReason()));
    }
}

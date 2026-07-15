package aptms.api;

import aptms.dto.vendor.CreateReinstatementRequestDTO;
import aptms.dto.vendor.ReinstatementRequestDTO;
import aptms.security.SecurityUtils;
import aptms.services.ReinstatementRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Vendor-facing reinstatement request flow for suspended vendor accounts.
 * Vendor identity is always resolved from the JWT, never a client-supplied ID.
 */
@RestController
@RequestMapping("/api/vendor/reinstatement-request")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
@Tag(name = "Vendor Reinstatement", description = "Suspended vendor requests to be reinstated")
public class VendorReinstatementController {

    private final ReinstatementRequestService reinstatementRequestService;

    @PostMapping
    @Operation(summary = "Submit a reinstatement request for the calling vendor's suspended account")
    public ResponseEntity<ReinstatementRequestDTO> createRequest(@Valid @RequestBody CreateReinstatementRequestDTO body) {
        ReinstatementRequestDTO dto = reinstatementRequestService.createRequest(
                SecurityUtils.getCurrentUserId(), body.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @Operation(summary = "Get the calling vendor's own reinstatement requests")
    public ResponseEntity<List<ReinstatementRequestDTO>> getMyRequests() {
        return ResponseEntity.ok(reinstatementRequestService.getMyRequests(SecurityUtils.getCurrentUserId()));
    }
}

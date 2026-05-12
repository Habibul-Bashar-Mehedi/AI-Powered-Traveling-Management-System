package aptms.api;

import aptms.dto.vendor.VendorServiceDTO;
import aptms.services.VendorServiceMgmtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for vendor service listing management.
 * Requirements: BRD §4.2 — Service Management
 */
@RestController
@RequestMapping("/api/v1/vendor/services")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
@Tag(name = "Vendor Services", description = "Service listing CRUD for vendors")
public class VendorServiceController {

    private final VendorServiceMgmtService serviceMgmtService;

    @GetMapping
    @Operation(summary = "List all services for authenticated vendor")
    public ResponseEntity<List<VendorServiceDTO>> getMyServices() {
        return ResponseEntity.ok(serviceMgmtService.getMyServices(getCurrentUserId()));
    }

    @PostMapping
    @Operation(summary = "Create a new service listing (FR-SVC-001)")
    public ResponseEntity<VendorServiceDTO> createService(@Valid @RequestBody VendorServiceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceMgmtService.createService(getCurrentUserId(), dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a service listing")
    public ResponseEntity<VendorServiceDTO> updateService(
            @PathVariable UUID id,
            @Valid @RequestBody VendorServiceDTO dto) {
        return ResponseEntity.ok(serviceMgmtService.updateService(getCurrentUserId(), id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a service listing")
    public ResponseEntity<Void> deleteService(@PathVariable UUID id) {
        serviceMgmtService.deleteService(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle service status (FR-SVC-005)")
    public ResponseEntity<VendorServiceDTO> toggleStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        return ResponseEntity.ok(serviceMgmtService.toggleServiceStatus(getCurrentUserId(), id, status));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}


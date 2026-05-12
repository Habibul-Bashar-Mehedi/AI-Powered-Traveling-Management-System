package aptms.api;

import aptms.dto.vendor.VendorProfileDTO;
import aptms.dto.vendor.VendorRegistrationRequest;
import aptms.services.VendorRegistrationService;
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

import java.util.UUID;

/**
 * REST controller for vendor registration and profile management.
 * Requirements: BRD FR-REG-001, FR-REG-006
 */
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
@Tag(name = "Vendor", description = "Vendor registration, onboarding and profile management")
public class VendorRegistrationController {

    private final VendorRegistrationService vendorRegistrationService;

    @PostMapping("/register")
    @Operation(summary = "Register as a vendor (public)", description = "FR-REG-001 — Multi-step vendor self-registration")
    public ResponseEntity<VendorProfileDTO> register(@Valid @RequestBody VendorRegistrationRequest request) {
        UUID userId = getCurrentUserId();
        VendorProfileDTO profile = vendorRegistrationService.register(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get own vendor profile")
    public ResponseEntity<VendorProfileDTO> getProfile() {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(vendorRegistrationService.getProfile(userId));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Update vendor profile")
    public ResponseEntity<VendorProfileDTO> updateProfile(@Valid @RequestBody VendorRegistrationRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(vendorRegistrationService.updateProfile(userId, request));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}


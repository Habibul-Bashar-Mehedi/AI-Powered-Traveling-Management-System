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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register as a vendor", description = "FR-REG-001 — Multi-step vendor self-registration. Requires an authenticated user (any role). The authenticated user's ID is used to create the vendor profile.")
    public ResponseEntity<VendorProfileDTO> register(@Valid @RequestBody VendorRegistrationRequest request) {
        UUID userId = getCurrentUserId();
        VendorProfileDTO profile = vendorRegistrationService.register(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get own vendor profile", description = "Returns the vendor profile associated with the currently authenticated vendor user.")
    public ResponseEntity<VendorProfileDTO> getProfile() {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(vendorRegistrationService.getProfile(userId));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Update vendor profile", description = "Replaces the vendor profile for the currently authenticated vendor user. Returns the updated profile.")
    public ResponseEntity<VendorProfileDTO> updateProfile(@Valid @RequestBody VendorRegistrationRequest request) {
        UUID userId = getCurrentUserId();
        return ResponseEntity.ok(vendorRegistrationService.updateProfile(userId, request));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            // Principal is a UserDetails object when JWT filter is active;
            // getName() delegates to UserDetails#getUsername() which stores the UUID string.
            Object principal = auth.getPrincipal();
            String name = (principal instanceof UserDetails ud) ? ud.getUsername() : auth.getName();
            return UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Cannot resolve user identity from authentication principal");
        }
    }
}


package aptms.api;

import aptms.dto.PackageBookRequestDTO;
import aptms.dto.PackageBookingDTO;
import aptms.dto.PackageDTO;
import aptms.services.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

/**
 * Traveler-facing catalog of published travel packages.
 */
@RestController
@RequestMapping("/api/v1/packages")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "Package Catalog", description = "Browse and book published travel packages")
public class PackageCatalogController {

    private final PackageService packageService;

    @GetMapping
    @Operation(summary = "List published travel packages")
    public ResponseEntity<Page<PackageDTO>> getPublishedPackages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("updatedAt").descending());
        return ResponseEntity.ok(packageService.getPublishedPackages(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a published package's full details for the View Details modal")
    public ResponseEntity<PackageDTO> getPackage(@PathVariable UUID id) {
        return ResponseEntity.ok(packageService.getPublishedPackageById(id));
    }

    @PostMapping("/{id}/book")
    @Operation(summary = "Book a published package — reserves every component in one transaction")
    public ResponseEntity<PackageBookingDTO> bookPackage(
            @PathVariable UUID id,
            @Valid @RequestBody PackageBookRequestDTO request) {
        PackageBookingDTO booking = packageService.bookPackage(getCurrentUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(Objects.requireNonNull(auth).getName());
    }
}

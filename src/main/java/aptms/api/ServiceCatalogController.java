package aptms.api;

import aptms.dto.vendor.BookServiceRequestDTO;
import aptms.dto.vendor.PublicServiceListingDTO;
import aptms.dto.vendor.ServiceAvailabilityDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.services.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Traveler-facing catalog of active vendor service listings — what a vendor
 * publishes (name, price, image) becomes visible here for users to browse and book.
 */
@RestController
@RequestMapping("/api/v1/services")
@PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "Browse and book active vendor service listings")
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    @Operation(summary = "List active vendor services available for booking (also used by admins to pick package components)")
    public ResponseEntity<Page<PublicServiceListingDTO>> getActiveServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by("updatedAt").descending());
        return ResponseEntity.ok(serviceCatalogService.getActiveServices(pageable));
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Check remaining bookable capacity for a service on a given date (or date range)")
    public ResponseEntity<ServiceAvailabilityDTO> getAvailability(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(serviceCatalogService.getAvailability(id, startDate, endDate));
    }

    @PostMapping("/{id}/book")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Book a specific active service listing")
    public ResponseEntity<VendorBookingDTO> bookService(
            @PathVariable UUID id,
            @Valid @RequestBody BookServiceRequestDTO request) {
        VendorBookingDTO booking = serviceCatalogService.bookService(getCurrentUserId(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(Objects.requireNonNull(auth).getName());
    }
}

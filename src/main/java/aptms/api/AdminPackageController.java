package aptms.api;

import aptms.dto.PackageDTO;
import aptms.enums.PackageStatus;
import aptms.services.FileStorageService;
import aptms.services.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for admin curation of Travel Packages — bundles of real
 * VendorService components (hotel room / transport route / tour package) sold
 * together at one price.
 */
@RestController
@RequestMapping("/api/v1/admin/packages")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Package Management", description = "Curate travel packages bundling real bookable services")
public class AdminPackageController {

    private final PackageService packageService;
    private final FileStorageService fileStorageService;

    @PostMapping("/images")
    @Operation(summary = "Upload a package image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file, "packages");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping
    @Operation(summary = "List all packages, including drafts and archived ones")
    public ResponseEntity<List<PackageDTO>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a package (any status) with its full component/extra breakdown")
    public ResponseEntity<PackageDTO> getPackage(@PathVariable UUID id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new package")
    public ResponseEntity<PackageDTO> createPackage(@Valid @RequestBody PackageDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(packageService.createPackage(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a package and replace its components/extras")
    public ResponseEntity<PackageDTO> updatePackage(@PathVariable UUID id, @Valid @RequestBody PackageDTO dto) {
        return ResponseEntity.ok(packageService.updatePackage(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a package")
    public ResponseEntity<Void> deletePackage(@PathVariable UUID id) {
        packageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition a package's status (publishing validates it's actually bookable)")
    public ResponseEntity<PackageDTO> setStatus(@PathVariable UUID id, @RequestParam PackageStatus status) {
        return ResponseEntity.ok(packageService.setStatus(id, status));
    }
}

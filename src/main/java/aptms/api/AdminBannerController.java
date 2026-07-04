package aptms.api;

import aptms.dto.BannerDTO;
import aptms.services.BannerService;
import aptms.services.FileStorageService;
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
 * REST controller for admin management of dashboard promotional banners.
 */
@RestController
@RequestMapping("/api/v1/admin/banners")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin — Banner Management", description = "Create and manage promotional banners shown on the user dashboard")
public class AdminBannerController {

    private final BannerService bannerService;
    private final FileStorageService fileStorageService;

    @PostMapping("/images")
    @Operation(summary = "Upload a banner image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeImage(file, "banners");
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping
    @Operation(summary = "List all banners, including inactive/scheduled ones")
    public ResponseEntity<List<BannerDTO>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping
    @Operation(summary = "Create a new banner")
    public ResponseEntity<BannerDTO> createBanner(@Valid @RequestBody BannerDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bannerService.createBanner(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a banner")
    public ResponseEntity<BannerDTO> updateBanner(@PathVariable UUID id, @Valid @RequestBody BannerDTO dto) {
        return ResponseEntity.ok(bannerService.updateBanner(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a banner")
    public ResponseEntity<Void> deleteBanner(@PathVariable UUID id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a banner")
    public ResponseEntity<BannerDTO> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(bannerService.setActive(id, active));
    }
}

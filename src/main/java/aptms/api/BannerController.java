package aptms.api;

import aptms.dto.BannerDTO;
import aptms.services.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (authenticated) endpoint for fetching currently-active dashboard banners.
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
@Tag(name = "Banners", description = "Read-only access to active promotional banners")
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/active")
    @Operation(summary = "Get currently active banners for the user dashboard")
    public ResponseEntity<List<BannerDTO>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }
}

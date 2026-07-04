package aptms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for admin-managed dashboard promotional banners.
 */
@Data
public class BannerDTO {

    private UUID id;

    @NotBlank
    @Size(max = 150)
    private String title;

    private String description;

    private String imageUrl;

    @Size(max = 40)
    private String badgeText;

    @Size(max = 40)
    private String ctaLabel = "Explore";

    @Size(max = 200)
    private String ctaTarget = "offers";

    private Boolean active = true;

    private Instant startDate;
    private Instant endDate;

    private Integer displayOrder = 0;

    private Instant createdAt;
    private Instant updatedAt;
}

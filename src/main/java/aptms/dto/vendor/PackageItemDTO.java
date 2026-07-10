package aptms.dto.vendor;

import aptms.enums.PackageItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** One itinerary leg of a TOUR_PACKAGE listing (e.g. "outbound transport", "hotel stay"). */
@Data
public class PackageItemDTO {

    private UUID itemId;

    @NotNull(message = "Item type is required")
    private PackageItemType itemType;

    @NotBlank(message = "Item title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    private Integer dayNumber;

    private Integer sequence = 0;
}

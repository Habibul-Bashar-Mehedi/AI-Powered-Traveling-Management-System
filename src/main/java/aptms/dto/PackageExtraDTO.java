package aptms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** Optional, purely descriptive add-on line shown alongside a Package. */
@Data
public class PackageExtraDTO {

    private UUID extraId;

    @NotBlank(message = "Extra title is required")
    @Size(max = 150)
    private String title;

    @Size(max = 2000)
    private String description;

    private BigDecimal price;

    private Boolean included = true;
}

package aptms.dto;

import aptms.enums.PackageStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** DTO for creating/updating/reading an admin-curated travel Package. */
@Data
public class PackageDTO {

    private UUID packageId;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    private String description;

    private String imageUrl;

    /** Optional — groups the package under an existing Destination. */
    private Long destinationId;
    private String destinationName;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalPrice;

    @Size(min = 3, max = 3)
    private String currencyCode = "USD";

    private PackageStatus status = PackageStatus.DRAFT;

    private Instant createdAt;
    private Instant updatedAt;

    @Valid
    private List<PackageComponentDTO> components = new ArrayList<>();

    @Valid
    private List<PackageExtraDTO> extras = new ArrayList<>();
}

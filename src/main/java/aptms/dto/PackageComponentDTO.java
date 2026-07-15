package aptms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** One real, bookable VendorService component of a Package. */
@Data
public class PackageComponentDTO {

    private UUID componentId;

    @NotNull(message = "A service is required for each package component")
    private UUID serviceId;

    /** Read-only, populated on responses only. */
    private String serviceName;
    private String serviceType;
    private BigDecimal basePrice;
    private String currencyCode;
    private Integer maxCapacity;

    @Min(1)
    private Integer quantity = 1;

    private Integer dayNumber;

    private Integer sequence = 0;

    private String notes;
}

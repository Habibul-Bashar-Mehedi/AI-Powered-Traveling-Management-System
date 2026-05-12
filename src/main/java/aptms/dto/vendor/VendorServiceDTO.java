package aptms.dto.vendor;

import aptms.enums.BookingMode;
import aptms.enums.PricingUnit;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating/updating a vendor service listing.
 * Requirements: BRD FR-SVC-001, FR-SVC-002
 */
@Data
public class VendorServiceDTO {

    private UUID serviceId;

    @NotBlank
    @Size(max = 255)
    private String serviceName;

    @NotNull
    private ServiceType serviceType;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    @Size(min = 3, max = 3)
    private String currencyCode = "USD";

    @NotNull
    private PricingUnit pricingUnit;

    @NotNull
    @Min(1)
    private Integer maxCapacity;

    private Integer minBookingNotice;
    private Integer maxBookingAdvance;

    private BookingMode bookingMode = BookingMode.MANUAL;
    private Integer confirmationWindow;

    private ServiceStatus status = ServiceStatus.DRAFT;

    private String cancellationPolicy;

    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String locationAddress;

    private String tags;
    private String metadata;

    private BigDecimal averageRating;
    private Integer totalBookings;
    private Boolean isFeatured;
    private Instant createdAt;
    private Instant updatedAt;
}


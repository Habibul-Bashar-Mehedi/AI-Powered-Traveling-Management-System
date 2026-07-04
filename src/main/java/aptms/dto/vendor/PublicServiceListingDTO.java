package aptms.dto.vendor;

import aptms.enums.PricingUnit;
import aptms.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only, traveler-facing view of an active vendor service listing.
 * Deliberately excludes vendor-internal management fields (bookingMode,
 * confirmationWindow, cancellationPolicy, metadata, etc.).
 */
@Data
public class PublicServiceListingDTO {
    private UUID serviceId;
    private String serviceName;
    private ServiceType serviceType;
    private String description;
    private BigDecimal basePrice;
    private String currencyCode;
    private PricingUnit pricingUnit;
    private String locationAddress;
    private String imageUrl;
    private BigDecimal averageRating;
    private Integer totalBookings;

    private UUID vendorId;
    private String vendorBusinessName;
}

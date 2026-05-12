package aptms.dto.vendor;

import aptms.enums.PayoutMethod;
import aptms.enums.VendorStatus;
import aptms.enums.VendorType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for vendor profile read/update responses.
 */
@Data
public class VendorProfileDTO {

    private UUID vendorId;
    private UUID userId;
    private String businessName;
    private VendorType vendorType;
    private String registrationNumber;
    private String taxId;
    private String description;
    private String logoUrl;
    private String email;
    private String phone;
    private String websiteUrl;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String countryCode;
    private String postalCode;
    private VendorStatus status;
    private String rejectionReason;
    private BigDecimal commissionRate;
    private BigDecimal walletBalance;
    private BigDecimal pendingBalance;
    private PayoutMethod payoutMethod;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Boolean isEmailVerified;
    private Instant createdAt;
    private Instant approvedAt;
}


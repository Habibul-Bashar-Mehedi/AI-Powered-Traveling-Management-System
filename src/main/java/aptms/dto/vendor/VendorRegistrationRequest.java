package aptms.dto.vendor;

import aptms.enums.VendorType;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for vendor self-registration (step 1 — business identity).
 * Requirements: BRD FR-REG-001
 */
@Data
public class VendorRegistrationRequest {

    @NotBlank
    @Size(max = 255)
    private String businessName;

    @NotNull
    private VendorType vendorType;

    @Size(max = 100)
    private String registrationNumber;

    @Size(max = 100)
    private String taxId;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 30)
    private String phone;

    @Size(max = 500)
    private String websiteUrl;

    @NotBlank
    @Size(max = 255)
    private String addressLine1;

    @Size(max = 255)
    private String addressLine2;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String stateProvince;

    @NotBlank
    @Size(min = 2, max = 2)
    private String countryCode;

    @Size(max = 20)
    private String postalCode;

    private String description;
}


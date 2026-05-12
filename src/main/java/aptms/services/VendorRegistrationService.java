package aptms.services;

import aptms.dto.vendor.VendorProfileDTO;
import aptms.dto.vendor.VendorRegistrationRequest;

import java.util.UUID;

/**
 * Service interface for vendor registration and profile management.
 */
public interface VendorRegistrationService {

    VendorProfileDTO register(VendorRegistrationRequest request, UUID userId);

    VendorProfileDTO getProfile(UUID userId);

    VendorProfileDTO updateProfile(UUID userId, VendorRegistrationRequest request);
}


package aptms.services;

import aptms.dto.vendor.VendorProfileDTO;
import aptms.dto.vendor.PayoutRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for admin vendor management.
 */
public interface AdminVendorService {

    List<VendorProfileDTO> getPendingVendors();

    List<VendorProfileDTO> getAllVendors();

    VendorProfileDTO approveVendor(UUID vendorId, UUID adminUserId);

    VendorProfileDTO rejectVendor(UUID vendorId, UUID adminUserId, String reason);

    VendorProfileDTO suspendVendor(UUID vendorId, UUID adminUserId, String reason);

    VendorProfileDTO reinstateVendor(UUID vendorId, UUID adminUserId);

    List<PayoutRequestDTO> getPendingPayouts();

    PayoutRequestDTO processePayout(UUID payoutId, UUID adminUserId, boolean approve, String note);
}


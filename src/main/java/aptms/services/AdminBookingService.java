package aptms.services;

import aptms.dto.vendor.UserBookingStatusSummaryDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.enums.VendorBookingStatus;

import java.util.List;

/**
 * Service interface for admin-wide booking visibility (read-only).
 */
public interface AdminBookingService {

    List<VendorBookingDTO> getAllBookings(VendorBookingStatus status);

    UserBookingStatusSummaryDTO getBookingStatusSummary();
}

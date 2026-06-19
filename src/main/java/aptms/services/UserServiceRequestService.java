package aptms.services;

import aptms.dto.vendor.UserServiceRequestDTO;
import aptms.dto.vendor.UserBookingStatusSummaryDTO;
import aptms.dto.vendor.VendorBookingDTO;

import aptms.enums.VendorBookingStatus;

import java.util.List;
import java.util.UUID;

public interface UserServiceRequestService {
    VendorBookingDTO createRequest(UUID userId, UserServiceRequestDTO request);

    List<VendorBookingDTO> getMyBookings(UUID userId, VendorBookingStatus status);

    UserBookingStatusSummaryDTO getMyBookingStatusSummary(UUID userId);

    VendorBookingDTO cancelMyBooking(UUID userId, UUID bookingId, String reason);
}



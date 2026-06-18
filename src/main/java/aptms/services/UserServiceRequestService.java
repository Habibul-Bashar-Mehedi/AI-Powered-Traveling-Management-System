package aptms.services;

import aptms.dto.vendor.UserServiceRequestDTO;
import aptms.dto.vendor.VendorBookingDTO;

import java.util.List;
import java.util.UUID;

public interface UserServiceRequestService {
    VendorBookingDTO createRequest(UUID userId, UserServiceRequestDTO request);
    List<VendorBookingDTO> getMyBookings(UUID userId);
}



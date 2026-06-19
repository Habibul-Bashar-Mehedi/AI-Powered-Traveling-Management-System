package aptms.services;

import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.User;
import aptms.entities.VendorBooking;
import aptms.enums.VendorBookingStatus;

import java.util.List;
import java.util.UUID;

public interface VendorBookingService {

    List<VendorBookingDTO> getBookings(UUID userId, VendorBookingStatus status);

    VendorBookingDTO getBookingDetail(UUID userId, UUID bookingId);

    VendorBookingDTO confirmBooking(UUID userId, UUID bookingId);

    VendorBookingDTO rejectBooking(UUID userId, UUID bookingId, String reason);

    VendorBookingDTO cancelBooking(UUID userId, UUID bookingId, String reason);

    /** Returns all bookings placed by a traveller (user-facing, not vendor-scoped). */
    List<VendorBookingDTO> getUserBookings(UUID userId, VendorBookingStatus status);

    VendorBookingDTO cancelUserBooking(UUID userId, UUID bookingId, String reason);

    VendorBookingDTO mapBookingForUser(VendorBooking booking, User user);
}


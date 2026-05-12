package aptms.services;

import aptms.dto.vendor.VendorBookingDTO;
import aptms.enums.VendorBookingStatus;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for vendor booking lifecycle management.
 */
public interface VendorBookingService {

    List<VendorBookingDTO> getBookings(UUID userId, VendorBookingStatus status);

    VendorBookingDTO getBookingDetail(UUID userId, UUID bookingId);

    VendorBookingDTO confirmBooking(UUID userId, UUID bookingId);

    VendorBookingDTO rejectBooking(UUID userId, UUID bookingId, String reason);

    VendorBookingDTO cancelBooking(UUID userId, UUID bookingId, String reason);
}


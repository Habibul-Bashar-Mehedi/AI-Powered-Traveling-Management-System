package aptms.services;

import aptms.dto.BookingHistoryDTO;
import aptms.entities.Booking;
import aptms.entities.VendorBooking;
import aptms.enums.BookingSource;
import aptms.enums.ServiceType;
import aptms.repositories.BookingRepository;
import aptms.repositories.VendorBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Merges a user's direct hotel {@code Booking} rows and generic
 * {@code VendorBooking} rows into a single, sorted, filterable history feed.
 *
 * <p>Note: a direct hotel {@code Booking} is auto-mirrored into a {@code VendorBooking}
 * (see {@link BookingService#booking}) so the hotel's vendor can see it in their inbox.
 * That mirrored row is not linked back to its source {@code Booking}, so a mirrored
 * hotel stay can appear here under both sources. This is a known, accepted tradeoff —
 * not a bug to silently "fix" by filtering one side out.
 */
@Service
@RequiredArgsConstructor
public class BookingHistoryService {

    private final BookingRepository bookingRepository;
    private final VendorBookingRepository vendorBookingRepository;

    @Transactional(readOnly = true)
    public Page<BookingHistoryDTO> getHistory(UUID userId, BookingSource typeFilter, String statusFilter, Pageable pageable) {
        List<BookingHistoryDTO> merged = mergedHistory(userId, typeFilter).stream()
                .filter(dto -> statusFilter == null || dto.getStatus().equalsIgnoreCase(statusFilter))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        if (start >= merged.size()) {
            return new PageImpl<>(List.of(), pageable, merged.size());
        }
        int end = Math.min(start + pageable.getPageSize(), merged.size());
        return new PageImpl<>(merged.subList(start, end), pageable, merged.size());
    }

    /** Full, unpaginated, unfiltered history for a user — used for cross-booking aggregation (e.g. spending-by-destination). */
    @Transactional(readOnly = true)
    public List<BookingHistoryDTO> getAllHistory(UUID userId) {
        return mergedHistory(userId, null);
    }

    private List<BookingHistoryDTO> mergedHistory(UUID userId, BookingSource typeFilter) {
        Stream<BookingHistoryDTO> hotelBookings = typeFilter == BookingSource.VENDOR_SERVICE
                ? Stream.empty()
                : bookingRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(userId).stream().map(this::mapBooking);

        Stream<BookingHistoryDTO> vendorBookings = typeFilter == BookingSource.HOTEL_DIRECT
                ? Stream.empty()
                : vendorBookingRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(userId).stream().map(this::mapVendorBooking);

        return Stream.concat(hotelBookings, vendorBookings)
                .sorted(Comparator.comparing(BookingHistoryDTO::getBookingDate).reversed())
                .collect(Collectors.toList());
    }

    private BookingHistoryDTO mapBooking(Booking booking) {
        BookingHistoryDTO dto = new BookingHistoryDTO();
        dto.setId(String.valueOf(booking.getId()));
        dto.setSource(BookingSource.HOTEL_DIRECT);
        dto.setServiceType(ServiceType.HOTEL_ROOM);
        dto.setTitle(booking.getHotel().getHotelName() + " — " + booking.getRoom().getRoomTypeName());
        dto.setBookingDate(booking.getCreatedAt().toInstant());
        dto.setTravelDate(booking.getCheckInDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        dto.setStatus(booking.getStatus().name());
        dto.setAmount(java.math.BigDecimal.valueOf(booking.getTotalPrice()));
        if (booking.getHotel().getDestination() != null) {
            dto.setDestinationId(booking.getHotel().getDestination().getId());
            dto.setDestinationName(booking.getHotel().getDestination().getName());
        }
        return dto;
    }

    private BookingHistoryDTO mapVendorBooking(VendorBooking booking) {
        BookingHistoryDTO dto = new BookingHistoryDTO();
        dto.setId(booking.getBookingId().toString());
        dto.setSource(BookingSource.VENDOR_SERVICE);
        dto.setServiceType(booking.getService().getServiceType());
        dto.setTitle(booking.getService().getServiceName());
        dto.setBookingDate(booking.getCreatedAt());
        dto.setTravelDate(booking.getStartDate());
        dto.setStatus(booking.getBookingStatus().name());
        dto.setAmount(booking.getGrossAmount());
        if (booking.getService().getDestination() != null) {
            dto.setDestinationId(booking.getService().getDestination().getId());
            dto.setDestinationName(booking.getService().getDestination().getName());
        }
        return dto;
    }
}

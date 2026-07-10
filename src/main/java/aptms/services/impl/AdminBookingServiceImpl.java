package aptms.services.impl;

import aptms.dto.vendor.UserBookingStatusSummaryDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.VendorBooking;
import aptms.enums.VendorBookingStatus;
import aptms.repositories.VendorBookingRepository;
import aptms.services.AdminBookingService;
import aptms.services.VendorBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {

    private final VendorBookingRepository bookingRepository;
    private final VendorBookingService vendorBookingService;

    @Override
    @Transactional(readOnly = true)
    public List<VendorBookingDTO> getAllBookings(VendorBookingStatus status) {
        List<VendorBooking> bookings = status == null
                ? bookingRepository.findAllWithDetailsOrderByCreatedAtDesc()
                : bookingRepository.findAllByStatusWithDetailsOrderByCreatedAtDesc(status);

        return bookings.stream()
                .map(b -> vendorBookingService.mapBookingForUser(b, b.getUser()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserBookingStatusSummaryDTO getBookingStatusSummary() {
        UserBookingStatusSummaryDTO summary = new UserBookingStatusSummaryDTO();
        EnumMap<VendorBookingStatus, Long> counts = new EnumMap<>(VendorBookingStatus.class);
        long total = 0L;

        for (Object[] row : bookingRepository.countAllGroupByStatus()) {
            VendorBookingStatus status = (VendorBookingStatus) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
            total += count;
        }

        summary.setCounts(counts);
        summary.setTotal(total);
        return summary;
    }
}

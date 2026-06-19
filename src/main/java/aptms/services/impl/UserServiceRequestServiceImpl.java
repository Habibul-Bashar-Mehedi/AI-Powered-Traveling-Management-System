package aptms.services.impl;

import aptms.dto.vendor.UserBookingStatusSummaryDTO;
import aptms.dto.vendor.UserServiceRequestDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import aptms.enums.UserServiceRequestType;
import aptms.enums.VendorBookingStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.UserServiceRequestService;
import aptms.services.VendorBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceRequestServiceImpl implements UserServiceRequestService {

    private final UserRepository userRepository;
    private final VendorServiceRepository vendorServiceRepository;
    private final VendorBookingRepository vendorBookingRepository;
    private final VendorBookingService vendorBookingService;

    @Override
    @Transactional
    public VendorBookingDTO createRequest(UUID userId, UserServiceRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdNotFoundException("User not found: " + userId));

        VendorService service = resolveService(request.getRequestType());
        Vendor vendor = service.getVendor();

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        BigDecimal grossAmount = service.getBasePrice().multiply(BigDecimal.valueOf(quantity));

        BigDecimal commissionRate = vendor.getCommissionRate() == null
                ? new BigDecimal("10.00")
                : vendor.getCommissionRate();
        BigDecimal commissionAmount = grossAmount
                .multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal netAmount = grossAmount.subtract(commissionAmount);

        VendorBooking booking = new VendorBooking();
        booking.setUser(user);
        booking.setVendor(vendor);
        booking.setService(service);
        booking.setStartDate(request.getStartDate() == null ? LocalDate.now().plusDays(1) : request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setQuantity(quantity);
        booking.setGrossAmount(grossAmount);
        booking.setCommissionAmount(commissionAmount);
        booking.setNetAmount(netAmount);

        String note = request.getSpecialRequests();
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            note = "Request: " + request.getTitle() + (note == null || note.isBlank() ? "" : " | " + note);
        }
        booking.setSpecialRequests(note);

        VendorBooking saved = vendorBookingRepository.save(booking);
        return vendorBookingService.mapBookingForUser(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBookingDTO> getMyBookings(UUID userId, VendorBookingStatus status) {
        return vendorBookingService.getUserBookings(userId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public UserBookingStatusSummaryDTO getMyBookingStatusSummary(UUID userId) {
        UserBookingStatusSummaryDTO summary = new UserBookingStatusSummaryDTO();
        EnumMap<VendorBookingStatus, Long> counts = new EnumMap<>(VendorBookingStatus.class);
        long total = 0L;

        for (Object[] row : vendorBookingRepository.countByUserIdGroupByStatus(userId)) {
            VendorBookingStatus status = (VendorBookingStatus) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(status, count);
            total += count;
        }

        summary.setCounts(counts);
        summary.setTotal(total);
        return summary;
    }

    @Override
    @Transactional
    public VendorBookingDTO cancelMyBooking(UUID userId, UUID bookingId, String reason) {
        return vendorBookingService.cancelUserBooking(userId, bookingId, reason);
    }

    private VendorService resolveService(UserServiceRequestType requestType) {
        ServiceType preferredType = switch (requestType) {
            case HOTEL_BOOKING -> ServiceType.HOTEL_ROOM;
            case EXPLORE_TOURIST_PLACES, ORDER_TRADITIONAL_FOOD_ITEMS -> ServiceType.TOUR_PACKAGE;
        };

        return vendorServiceRepository
                .findFirstByServiceTypeAndStatusOrderByUpdatedAtDesc(preferredType, ServiceStatus.ACTIVE)
                .or(() -> vendorServiceRepository.findFirstByStatusOrderByUpdatedAtDesc(ServiceStatus.ACTIVE))
                .orElseThrow(() -> new IllegalStateException(
                        "No active vendor service found. Ask a vendor to publish an ACTIVE service first."));
    }
}

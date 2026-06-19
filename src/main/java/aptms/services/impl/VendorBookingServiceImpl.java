package aptms.services.impl;

import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.enums.CancelledBy;
import aptms.enums.VendorBookingStatus;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorRepository;
import aptms.services.VendorBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorBookingServiceImpl implements VendorBookingService {

    private final VendorBookingRepository bookingRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VendorBookingDTO> getBookings(UUID userId, VendorBookingStatus status) {
        Vendor vendor = getVendorByUserId(userId);
        List<VendorBooking> bookings = status == null
                ? bookingRepository.findByVendorVendorIdWithDetailsOrderByCreatedAtDesc(vendor.getVendorId())
                : bookingRepository.findByVendorVendorIdAndStatusWithDetailsOrderByCreatedAtDesc(
                        vendor.getVendorId(), status);
        return bookings.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VendorBookingDTO getBookingDetail(UUID userId, UUID bookingId) {
        Vendor vendor = getVendorByUserId(userId);
        VendorBooking booking = bookingRepository
                .findByBookingIdAndVendorVendorId(bookingId, vendor.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDTO(booking);
    }

    @Override
    @Transactional
    public VendorBookingDTO confirmBooking(UUID userId, UUID bookingId) {
        Vendor vendor = getVendorByUserId(userId);
        VendorBooking booking = getBookingForVendor(bookingId, vendor.getVendorId());

        if (booking.getBookingStatus() != VendorBookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING state");
        }
        booking.setBookingStatus(VendorBookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());

        // Add net amount to vendor pending balance
        vendor.setPendingBalance(vendor.getPendingBalance().add(booking.getNetAmount()));
        vendorRepository.save(vendor);

        log.info("Booking {} confirmed by vendor {}", bookingId, vendor.getVendorId());
        return toDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public VendorBookingDTO rejectBooking(UUID userId, UUID bookingId, String reason) {
        Vendor vendor = getVendorByUserId(userId);
        VendorBooking booking = getBookingForVendor(bookingId, vendor.getVendorId());

        if (booking.getBookingStatus() != VendorBookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not in PENDING state");
        }
        booking.setBookingStatus(VendorBookingStatus.REJECTED);
        booking.setCancellationReason(reason);
        booking.setCancelledBy(CancelledBy.VENDOR);

        log.info("Booking {} rejected by vendor {}", bookingId, vendor.getVendorId());
        return toDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public VendorBookingDTO cancelBooking(UUID userId, UUID bookingId, String reason) {
        Vendor vendor = getVendorByUserId(userId);
        VendorBooking booking = getBookingForVendor(bookingId, vendor.getVendorId());

        if (booking.getBookingStatus() == VendorBookingStatus.CANCELLED ||
                booking.getBookingStatus() == VendorBookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking cannot be cancelled in its current state");
        }
        booking.setBookingStatus(VendorBookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledBy(CancelledBy.VENDOR);

        // Reverse pending balance if was confirmed
        if (booking.getConfirmedAt() != null) {
            vendor.setPendingBalance(vendor.getPendingBalance().subtract(booking.getNetAmount()));
            vendorRepository.save(vendor);
        }

        log.info("Booking {} cancelled by vendor {}", bookingId, vendor.getVendorId());
        return toDTO(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBookingDTO> getUserBookings(UUID userId, VendorBookingStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<VendorBooking> bookings = status == null
                ? bookingRepository.findByUserIdWithDetailsOrderByCreatedAtDesc(userId)
                : bookingRepository.findByUserIdAndStatusWithDetailsOrderByCreatedAtDesc(userId, status);

        return bookings.stream()
                .map(booking -> toUserDTO(booking, user))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VendorBookingDTO cancelUserBooking(UUID userId, UUID bookingId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        VendorBooking booking = bookingRepository.findByBookingIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getBookingStatus() != VendorBookingStatus.PENDING
                && booking.getBookingStatus() != VendorBookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking cannot be cancelled in its current state");
        }

        if (booking.getBookingStatus() == VendorBookingStatus.CONFIRMED) {
            Vendor vendor = booking.getVendor();
            vendor.setPendingBalance(vendor.getPendingBalance().subtract(booking.getNetAmount()));
            vendorRepository.save(vendor);
        }

        booking.setBookingStatus(VendorBookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledBy(CancelledBy.USER);

        log.info("Booking {} cancelled by user {}", bookingId, userId);
        return toUserDTO(bookingRepository.save(booking), user);
    }

    @Override
    public VendorBookingDTO mapBookingForUser(VendorBooking booking, User user) {
        return toUserDTO(booking, user);
    }

    private Vendor getVendorByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user: " + userId));
    }

    private VendorBooking getBookingForVendor(UUID bookingId, UUID vendorId) {
        return bookingRepository.findByBookingIdAndVendorVendorId(bookingId, vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    public VendorBookingDTO toDTO(VendorBooking b) {
        return toUserDTO(b, b.getUser());
    }

    public VendorBookingDTO toUserDTO(VendorBooking b, User user) {
        VendorBookingDTO dto = new VendorBookingDTO();
        dto.setBookingId(b.getBookingId());
        dto.setServiceId(b.getService() != null ? b.getService().getServiceId() : null);
        dto.setServiceName(b.getService() != null ? b.getService().getServiceName() : null);
        dto.setVendorId(b.getVendor() != null ? b.getVendor().getVendorId() : null);
        dto.setUserId(user != null ? user.getId() : null);
        dto.setUserName(user != null ? user.getUsername() : null);
        dto.setUserEmail(user != null ? user.getEmail() : null);
        dto.setBookingStatus(b.getBookingStatus());
        dto.setStartDate(b.getStartDate());
        dto.setEndDate(b.getEndDate());
        dto.setQuantity(b.getQuantity());
        dto.setGrossAmount(b.getGrossAmount());
        dto.setCommissionAmount(b.getCommissionAmount());
        dto.setNetAmount(b.getNetAmount());
        dto.setPaymentStatus(b.getPaymentStatus());
        dto.setSpecialRequests(b.getSpecialRequests());
        dto.setCancellationReason(b.getCancellationReason());
        dto.setCancelledBy(b.getCancelledBy());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setConfirmedAt(b.getConfirmedAt());
        dto.setCompletedAt(b.getCompletedAt());
        return dto;
    }
}


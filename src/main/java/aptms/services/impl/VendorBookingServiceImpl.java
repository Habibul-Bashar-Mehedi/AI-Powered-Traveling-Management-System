package aptms.services.impl;

import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.enums.CancelledBy;
import aptms.enums.VendorBookingStatus;
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

    @Override
    @Transactional(readOnly = true)
    public List<VendorBookingDTO> getBookings(UUID userId, VendorBookingStatus status) {
        Vendor vendor = getVendorByUserId(userId);
        List<VendorBooking> bookings = status == null
                ? bookingRepository.findByVendorVendorIdOrderByCreatedAtDesc(vendor.getVendorId())
                : bookingRepository.findByVendorVendorIdAndBookingStatusOrderByCreatedAtDesc(vendor.getVendorId(), status);
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
    public List<VendorBookingDTO> getUserBookings(UUID userId) {
        return bookingRepository
                .findByUserIdWithDetailsOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
        VendorBookingDTO dto = new VendorBookingDTO();
        dto.setBookingId(b.getBookingId());
        dto.setServiceId(b.getService() != null ? b.getService().getServiceId() : null);
        dto.setServiceName(b.getService() != null ? b.getService().getServiceName() : null);
        dto.setVendorId(b.getVendor() != null ? b.getVendor().getVendorId() : null);
        dto.setUserId(b.getUser() != null ? b.getUser().getId() : null);
        dto.setUserName(b.getUser() != null ? b.getUser().getUsername() : null);
        dto.setUserEmail(b.getUser() != null ? b.getUser().getEmail() : null);
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


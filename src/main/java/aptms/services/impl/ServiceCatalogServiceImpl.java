package aptms.services.impl;

import aptms.dto.vendor.BookServiceRequestDTO;
import aptms.dto.vendor.PackageItemDTO;
import aptms.dto.vendor.PublicServiceListingDTO;
import aptms.dto.vendor.ServiceAvailabilityDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.PackageItem;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.entities.VendorService;
import aptms.enums.PaymentMethod;
import aptms.enums.ServiceStatus;
import aptms.enums.VendorPaymentStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.ServiceCatalogService;
import aptms.services.VendorBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final VendorServiceRepository vendorServiceRepository;
    private final UserRepository userRepository;
    private final VendorBookingRepository vendorBookingRepository;
    private final VendorBookingService vendorBookingService;

    @Override
    @Transactional(readOnly = true)
    public Page<PublicServiceListingDTO> getActiveServices(Pageable pageable) {
        return vendorServiceRepository.findByStatus(ServiceStatus.ACTIVE, pageable).map(this::toPublicDTO);
    }

    @Override
    @Transactional
    public VendorBookingDTO bookService(UUID userId, UUID serviceId, BookServiceRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdNotFoundException("User not found: " + userId));

        // Pessimistic lock on the service row: serializes concurrent booking attempts for
        // this listing so two travelers can't both read "1 seat left" and both book it —
        // the second transaction blocks here until the first commits, then re-checks capacity.
        VendorService service = vendorServiceRepository
                .findByServiceIdAndStatusForUpdate(serviceId, ServiceStatus.ACTIVE)
                .orElseThrow(() -> new IdNotFoundException("Active service not found: " + serviceId));

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : startDate;
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before the start date.");
        }

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity > service.getMaxCapacity()) {
            throw new IllegalArgumentException(
                    "Quantity exceeds the maximum of " + service.getMaxCapacity() + " for this listing.");
        }

        int alreadyBooked = vendorBookingRepository.sumBookedQuantityForDateRange(serviceId, startDate, endDate);
        int available = service.getMaxCapacity() - alreadyBooked;
        if (quantity > available) {
            throw new IllegalArgumentException(available <= 0
                    ? "This service is fully booked for the selected date(s)."
                    : "Only " + available + " left for the selected date(s).");
        }

        if (isMobileWallet(request.getPaymentMethod()) && !request.getPaymentReference().matches("01[3-9]\\d{8}")) {
            throw new IllegalArgumentException(
                    "Enter a valid 11-digit " + request.getPaymentMethod() + " number (e.g. 017XXXXXXXX).");
        }

        Vendor vendor = service.getVendor();
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
        booking.setStartDate(startDate);
        booking.setEndDate(request.getEndDate());
        booking.setQuantity(quantity);
        booking.setGrossAmount(grossAmount);
        booking.setCommissionAmount(commissionAmount);
        booking.setNetAmount(netAmount);
        booking.setSpecialRequests(request.getSpecialRequests());

        // Simulated checkout: this project has no live bKash/Rocket/Nagad/bank merchant
        // integration, so the chosen method is recorded and payment is marked settled
        // immediately rather than left PENDING against a gateway that doesn't exist.
        booking.setPaymentMethod(request.getPaymentMethod());
        booking.setPaymentReference(request.getPaymentReference());
        booking.setPaymentStatus(VendorPaymentStatus.PAID);

        VendorBooking saved = vendorBookingRepository.save(booking);
        return vendorBookingService.mapBookingForUser(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceAvailabilityDTO getAvailability(UUID serviceId, LocalDate startDate, LocalDate endDate) {
        VendorService service = vendorServiceRepository.findByServiceIdAndStatus(serviceId, ServiceStatus.ACTIVE)
                .orElseThrow(() -> new IdNotFoundException("Active service not found: " + serviceId));

        LocalDate effectiveEnd = endDate != null ? endDate : startDate;
        if (effectiveEnd.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before the start date.");
        }

        int alreadyBooked = vendorBookingRepository.sumBookedQuantityForDateRange(serviceId, startDate, effectiveEnd);
        int available = Math.max(0, service.getMaxCapacity() - alreadyBooked);
        return new ServiceAvailabilityDTO(service.getMaxCapacity(), alreadyBooked, available);
    }

    private boolean isMobileWallet(PaymentMethod method) {
        return method == PaymentMethod.BKASH || method == PaymentMethod.ROCKET || method == PaymentMethod.NAGAD;
    }

    private PublicServiceListingDTO toPublicDTO(VendorService e) {
        PublicServiceListingDTO dto = new PublicServiceListingDTO();
        dto.setServiceId(e.getServiceId());
        dto.setServiceName(e.getServiceName());
        dto.setServiceType(e.getServiceType());
        dto.setDescription(e.getDescription());
        dto.setBasePrice(e.getBasePrice());
        dto.setCurrencyCode(e.getCurrencyCode());
        dto.setPricingUnit(e.getPricingUnit());
        dto.setLocationAddress(e.getLocationAddress());
        dto.setImageUrl(e.getImageUrl());
        dto.setAverageRating(e.getAverageRating());
        dto.setTotalBookings(e.getTotalBookings());
        dto.setMaxCapacity(e.getMaxCapacity());
        dto.setPackageItems(e.getPackageItems().stream().map(this::toItemDTO).collect(Collectors.toList()));

        Vendor vendor = e.getVendor();
        dto.setVendorId(vendor.getVendorId());
        dto.setVendorBusinessName(vendor.getBusinessName());
        return dto;
    }

    private PackageItemDTO toItemDTO(PackageItem item) {
        PackageItemDTO dto = new PackageItemDTO();
        dto.setItemId(item.getItemId());
        dto.setItemType(item.getItemType());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setDayNumber(item.getDayNumber());
        dto.setSequence(item.getSequence());
        return dto;
    }
}

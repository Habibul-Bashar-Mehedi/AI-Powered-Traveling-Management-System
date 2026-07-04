package aptms.services.impl;

import aptms.dto.vendor.BookServiceRequestDTO;
import aptms.dto.vendor.PublicServiceListingDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
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

        VendorService service = vendorServiceRepository.findByServiceIdAndStatus(serviceId, ServiceStatus.ACTIVE)
                .orElseThrow(() -> new IdNotFoundException("Active service not found: " + serviceId));

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
        booking.setSpecialRequests(request.getSpecialRequests());

        VendorBooking saved = vendorBookingRepository.save(booking);
        return vendorBookingService.mapBookingForUser(saved, user);
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

        Vendor vendor = e.getVendor();
        dto.setVendorId(vendor.getVendorId());
        dto.setVendorBusinessName(vendor.getBusinessName());
        return dto;
    }
}

package aptms.services;

import aptms.dto.vendor.BookServiceRequestDTO;
import aptms.dto.vendor.PublicServiceListingDTO;
import aptms.dto.vendor.ServiceAvailabilityDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.User;
import aptms.entities.VendorBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ServiceCatalogService {
    Page<PublicServiceListingDTO> getActiveServices(Pageable pageable);

    VendorBookingDTO bookService(UUID userId, UUID serviceId, BookServiceRequestDTO request);

    ServiceAvailabilityDTO getAvailability(UUID serviceId, LocalDate startDate, LocalDate endDate);

    /**
     * Locks, validates, and builds (unsaved) a VendorBooking reserving capacity on one
     * VendorService. Shared by single-service booking and PackageServiceImpl's
     * multi-component package booking. See impl for details.
     */
    VendorBooking reserveComponent(User user, UUID serviceId, LocalDate startDate, LocalDate endDate,
                                    int quantity, String specialRequests);
}

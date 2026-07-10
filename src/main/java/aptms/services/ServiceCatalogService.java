package aptms.services;

import aptms.dto.vendor.BookServiceRequestDTO;
import aptms.dto.vendor.PublicServiceListingDTO;
import aptms.dto.vendor.ServiceAvailabilityDTO;
import aptms.dto.vendor.VendorBookingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ServiceCatalogService {
    Page<PublicServiceListingDTO> getActiveServices(Pageable pageable);

    VendorBookingDTO bookService(UUID userId, UUID serviceId, BookServiceRequestDTO request);

    ServiceAvailabilityDTO getAvailability(UUID serviceId, LocalDate startDate, LocalDate endDate);
}

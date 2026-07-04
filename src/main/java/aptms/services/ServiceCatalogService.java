package aptms.services;

import aptms.dto.vendor.BookServiceRequestDTO;
import aptms.dto.vendor.PublicServiceListingDTO;
import aptms.dto.vendor.VendorBookingDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceCatalogService {
    Page<PublicServiceListingDTO> getActiveServices(Pageable pageable);

    VendorBookingDTO bookService(UUID userId, UUID serviceId, BookServiceRequestDTO request);
}

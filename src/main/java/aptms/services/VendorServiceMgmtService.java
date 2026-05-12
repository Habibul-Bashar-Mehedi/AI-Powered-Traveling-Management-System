package aptms.services;

import aptms.dto.vendor.VendorServiceDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for vendor service listing management.
 */
public interface VendorServiceMgmtService {

    List<VendorServiceDTO> getMyServices(UUID userId);

    VendorServiceDTO createService(UUID userId, VendorServiceDTO dto);

    VendorServiceDTO updateService(UUID userId, UUID serviceId, VendorServiceDTO dto);

    void deleteService(UUID userId, UUID serviceId);

    VendorServiceDTO toggleServiceStatus(UUID userId, UUID serviceId, String status);
}


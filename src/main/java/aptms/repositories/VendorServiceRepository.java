package aptms.repositories;

import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorServiceRepository extends JpaRepository<VendorService, UUID> {

    List<VendorService> findByVendorVendorId(UUID vendorId);

    List<VendorService> findByVendorVendorIdAndStatus(UUID vendorId, ServiceStatus status);

    Optional<VendorService> findByServiceIdAndVendorVendorId(UUID serviceId, UUID vendorId);

    long countByVendorVendorIdAndStatus(UUID vendorId, ServiceStatus status);
}


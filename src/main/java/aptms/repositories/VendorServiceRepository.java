package aptms.repositories;

import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorServiceRepository extends JpaRepository<VendorService, UUID> {

    List<VendorService> findByVendorVendorId(UUID vendorId);

    Page<VendorService> findByStatus(ServiceStatus status, Pageable pageable);

    Page<VendorService> findByStatusAndServiceType(ServiceStatus status, ServiceType serviceType, Pageable pageable);

    Optional<VendorService> findByServiceIdAndStatus(UUID serviceId, ServiceStatus status);

    List<VendorService> findByVendorVendorIdAndStatus(UUID vendorId, ServiceStatus status);

    Optional<VendorService> findByServiceIdAndVendorVendorId(UUID serviceId, UUID vendorId);

    Optional<VendorService> findFirstByServiceTypeAndStatusOrderByUpdatedAtDesc(
            ServiceType serviceType, ServiceStatus status);

    Optional<VendorService> findFirstByStatusOrderByUpdatedAtDesc(ServiceStatus status);

    Optional<VendorService> findFirstByVendorVendorIdAndServiceType(UUID vendorId, ServiceType serviceType);

    long countByVendorVendorIdAndStatus(UUID vendorId, ServiceStatus status);
    
    @Modifying
    @Query("DELETE FROM VendorService vs WHERE vs.vendor.vendorId = :vendorId")
    void deleteByVendorId(@Param("vendorId") UUID vendorId);
}


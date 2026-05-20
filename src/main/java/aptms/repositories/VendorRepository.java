package aptms.repositories;

import aptms.entities.Vendor;
import aptms.entities.User;
import aptms.enums.VendorStatus;
import aptms.repositories.projections.VendorAnalyticsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByUser(User user);

    Optional<Vendor> findByEmail(String email);

    List<Vendor> findByStatus(VendorStatus status);

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByTaxId(String taxId);

    @Query("SELECT v FROM Vendor v WHERE v.user.id = :userId")
    Optional<Vendor> findByUserId(UUID userId);

    @EntityGraph(attributePaths = {"user", "approvedBy"})
    @Query("""
            SELECT v FROM Vendor v
            WHERE v.status IN :statuses
            AND (:search IS NULL
                 OR LOWER(v.businessName) LIKE :search
                 OR LOWER(v.email) LIKE :search)
            """)
    Page<Vendor> searchByStatuses(@Param("statuses") List<VendorStatus> statuses,
                                  @Param("search") String search,
                                  Pageable pageable);

    @Query("""
            SELECT
              COUNT(v) AS totalVendors,
              COALESCE(SUM(CASE WHEN v.status IN :pendingStatuses THEN 1 ELSE 0 END), 0) AS pendingVendors,
              COALESCE(SUM(CASE WHEN v.status = aptms.enums.VendorStatus.APPROVED THEN 1 ELSE 0 END), 0) AS approvedVendors,
              COALESCE(SUM(CASE WHEN v.status = aptms.enums.VendorStatus.REJECTED THEN 1 ELSE 0 END), 0) AS rejectedVendors,
              COALESCE(SUM(CASE WHEN v.status = aptms.enums.VendorStatus.SUSPENDED THEN 1 ELSE 0 END), 0) AS suspendedVendors
            FROM Vendor v
            """)
    VendorAnalyticsProjection fetchVendorAnalytics(@Param("pendingStatuses") List<VendorStatus> pendingStatuses);
}


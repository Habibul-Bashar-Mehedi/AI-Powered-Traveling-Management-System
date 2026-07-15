package aptms.repositories;

import aptms.entities.ReinstatementRequest;
import aptms.enums.ReinstatementStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReinstatementRequestRepository extends JpaRepository<ReinstatementRequest, UUID> {

    boolean existsByVendorVendorIdAndStatus(UUID vendorId, ReinstatementStatus status);

    List<ReinstatementRequest> findByVendorVendorIdOrderBySubmittedAtDesc(UUID vendorId);

    @EntityGraph(attributePaths = {"vendor", "reviewedBy"})
    @Query("SELECT r FROM ReinstatementRequest r ORDER BY r.submittedAt DESC")
    List<ReinstatementRequest> findAllWithDetailsOrderBySubmittedAtDesc();

    @EntityGraph(attributePaths = {"vendor", "reviewedBy"})
    @Query("SELECT r FROM ReinstatementRequest r WHERE r.status = :status ORDER BY r.submittedAt DESC")
    List<ReinstatementRequest> findByStatusWithDetailsOrderBySubmittedAtDesc(@Param("status") ReinstatementStatus status);
}

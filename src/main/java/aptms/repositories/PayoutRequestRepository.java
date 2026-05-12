package aptms.repositories;

import aptms.entities.PayoutRequest;
import aptms.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, UUID> {

    List<PayoutRequest> findByVendorVendorIdOrderByRequestedAtDesc(UUID vendorId);

    List<PayoutRequest> findByStatus(PayoutStatus status);
}


package aptms.repositories;

import aptms.entities.PackageBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PackageBookingRepository extends JpaRepository<PackageBooking, UUID> {

    @Query("""
        SELECT pb FROM PackageBooking pb
        JOIN FETCH pb.packageEntity p
        WHERE pb.user.id = :userId
        ORDER BY pb.createdAt DESC
    """)
    List<PackageBooking> findByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") UUID userId);

    /** Package bookings still unpaid past a cutoff — candidates for expiry. */
    @Query("""
        SELECT pb FROM PackageBooking pb
        WHERE pb.paymentStatus = aptms.enums.VendorPaymentStatus.PENDING
          AND pb.createdAt < :cutoff
    """)
    List<PackageBooking> findStalePendingPayments(@Param("cutoff") Instant cutoff);
}

package aptms.repositories;

import aptms.entities.VendorBooking;
import aptms.enums.VendorBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorBookingRepository extends JpaRepository<VendorBooking, UUID> {

    List<VendorBooking> findByVendorVendorIdOrderByCreatedAtDesc(UUID vendorId);

    List<VendorBooking> findByVendorVendorIdAndBookingStatusOrderByCreatedAtDesc(
            UUID vendorId, VendorBookingStatus status);

    Optional<VendorBooking> findByBookingIdAndVendorVendorId(UUID bookingId, UUID vendorId);

    long countByVendorVendorIdAndBookingStatus(UUID vendorId, VendorBookingStatus status);

    @Query("SELECT SUM(b.netAmount) FROM VendorBooking b WHERE b.vendor.vendorId = :vendorId AND b.bookingStatus = 'COMPLETED'")
    java.math.BigDecimal sumNetAmountByVendorId(UUID vendorId);

    @Query("SELECT b FROM VendorBooking b WHERE b.bookingStatus = 'CONFIRMED' AND b.completedAt IS NULL AND b.endDate < CURRENT_DATE")
    List<VendorBooking> findBookingsToComplete();

    List<VendorBooking> findByVendorVendorIdAndCreatedAtBetween(UUID vendorId, Instant from, Instant to);

    /** Returns all bookings submitted by a specific user, newest first — eagerly fetches vendor+service+user. */
    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.vendor v
        JOIN FETCH v.user vu
        JOIN FETCH b.service s
        JOIN FETCH b.user u
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") UUID userId);
}


package aptms.repositories;

import aptms.entities.VendorBooking;
import aptms.enums.VendorBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorBookingRepository extends JpaRepository<VendorBooking, UUID> {

    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.service s
        JOIN FETCH b.user u
        WHERE b.vendor.vendorId = :vendorId
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findByVendorVendorIdWithDetailsOrderByCreatedAtDesc(@Param("vendorId") UUID vendorId);

    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.service s
        JOIN FETCH b.user u
        WHERE b.vendor.vendorId = :vendorId AND b.bookingStatus = :status
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findByVendorVendorIdAndStatusWithDetailsOrderByCreatedAtDesc(
            @Param("vendorId") UUID vendorId,
            @Param("status") VendorBookingStatus status);

    Optional<VendorBooking> findByBookingIdAndVendorVendorId(UUID bookingId, UUID vendorId);

    long countByVendorVendorIdAndBookingStatus(UUID vendorId, VendorBookingStatus status);

    @Query("SELECT SUM(b.netAmount) FROM VendorBooking b WHERE b.vendor.vendorId = :vendorId AND b.bookingStatus = 'COMPLETED'")
    java.math.BigDecimal sumNetAmountByVendorId(UUID vendorId);

    @Query("SELECT b FROM VendorBooking b WHERE b.bookingStatus = 'CONFIRMED' AND b.completedAt IS NULL AND b.endDate < CURRENT_DATE")
    List<VendorBooking> findBookingsToComplete();

    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.vendor v
        WHERE b.bookingStatus = 'COMPLETED'
          AND b.completedAt IS NOT NULL
          AND b.completedAt < :cutoff
          AND b.netAmount > 0
    """)
    List<VendorBooking> findSettleableBookings(@Param("cutoff") Instant cutoff);

    List<VendorBooking> findByVendorVendorIdAndCreatedAtBetween(UUID vendorId, Instant from, Instant to);

    /** User dashboard: service + vendor only (user row is known from auth context). */
    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.service s
        JOIN FETCH b.vendor v
        LEFT JOIN FETCH s.destination
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.service s
        JOIN FETCH b.vendor v
        WHERE b.user.id = :userId AND b.bookingStatus = :status
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findByUserIdAndStatusWithDetailsOrderByCreatedAtDesc(
            @Param("userId") UUID userId,
            @Param("status") VendorBookingStatus status);

    @Query("""
        SELECT b.bookingStatus, COUNT(b)
        FROM VendorBooking b
        WHERE b.user.id = :userId
        GROUP BY b.bookingStatus
    """)
    List<Object[]> countByUserIdGroupByStatus(@Param("userId") UUID userId);

    Optional<VendorBooking> findByBookingIdAndUserId(UUID bookingId, UUID userId);

    boolean existsByServiceServiceId(UUID serviceId);

    /**
     * Total quantity already booked (excluding cancelled/rejected) for a service
     * across any existing booking whose date range overlaps [startDate, endDate].
     * Single-day bookings store endDate = NULL, so they're treated as occupying
     * just their startDate via COALESCE.
     */
    @Query("""
        SELECT COALESCE(SUM(b.quantity), 0) FROM VendorBooking b
        WHERE b.service.serviceId = :serviceId
          AND b.bookingStatus NOT IN (aptms.enums.VendorBookingStatus.CANCELLED, aptms.enums.VendorBookingStatus.REJECTED)
          AND b.startDate <= :endDate
          AND COALESCE(b.endDate, b.startDate) >= :startDate
    """)
    int sumBookedQuantityForDateRange(
            @Param("serviceId") UUID serviceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /** Admin-wide booking inbox: every booking across all vendors and users. */
    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.service s
        JOIN FETCH b.vendor v
        JOIN FETCH b.user u
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findAllWithDetailsOrderByCreatedAtDesc();

    @Query("""
        SELECT b FROM VendorBooking b
        JOIN FETCH b.service s
        JOIN FETCH b.vendor v
        JOIN FETCH b.user u
        WHERE b.bookingStatus = :status
        ORDER BY b.createdAt DESC
    """)
    List<VendorBooking> findAllByStatusWithDetailsOrderByCreatedAtDesc(@Param("status") VendorBookingStatus status);

    @Query("SELECT b.bookingStatus, COUNT(b) FROM VendorBooking b GROUP BY b.bookingStatus")
    List<Object[]> countAllGroupByStatus();

    @Modifying
    @Query("DELETE FROM VendorBooking vb WHERE vb.vendor.vendorId = :vendorId")
    void deleteByVendorId(@Param("vendorId") UUID vendorId);

    /** All component bookings belonging to one package booking (for payment resolution/cancellation). */
    List<VendorBooking> findByPackageBooking_PackageBookingId(UUID packageBookingId);

    /** Single-service bookings still unpaid past a cutoff — candidates for expiry (not part of a package). */
    @Query("""
        SELECT b FROM VendorBooking b
        WHERE b.paymentStatus = aptms.enums.VendorPaymentStatus.PENDING
          AND b.bookingStatus = aptms.enums.VendorBookingStatus.PENDING
          AND b.packageBooking IS NULL
          AND b.createdAt < :cutoff
    """)
    List<VendorBooking> findStalePendingPayments(@Param("cutoff") Instant cutoff);
}


package aptms.repositories;

import aptms.entities.VendorBooking;
import aptms.enums.VendorBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}


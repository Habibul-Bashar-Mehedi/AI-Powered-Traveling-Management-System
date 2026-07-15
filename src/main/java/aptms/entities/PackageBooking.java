package aptms.entities;

import aptms.enums.PaymentMethod;
import aptms.enums.VendorPaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Header grouping the per-component VendorBooking rows created when a traveler
 * books a Package in one transaction. Each component keeps flowing through the
 * existing vendor booking inbox/confirm/reject/cancel machinery unmodified —
 * this just lets the traveler's "my bookings" view group them back together.
 */
@Entity
@Table(name = "package_booking")
@Data
public class PackageBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID packageBookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package packageEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Anchors every component's date range via its dayNumber offset. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "total_gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGrossAmount;

    @Column(name = "total_commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCommissionAmount;

    @Column(name = "total_net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 25)
    private VendorPaymentStatus paymentStatus = VendorPaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

package aptms.entities;

import aptms.enums.CancelledBy;
import aptms.enums.PaymentMethod;
import aptms.enums.VendorBookingStatus;
import aptms.enums.VendorPaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * VendorBooking entity for the vendor-centric booking lifecycle.
 * Requirements: BRD §6.3 — booking table (vendor-relevant fields)
 */
@Entity
@Table(name = "vendor_booking")
@Data
public class VendorBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private VendorService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Set only when this booking was created as one component of a Package booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_booking_id")
    private PackageBooking packageBooking;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    private VendorBookingStatus bookingStatus = VendorBookingStatus.PENDING;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 25)
    private VendorPaymentStatus paymentStatus = VendorPaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    /** Delivery address for orders that need to be sent to the customer (e.g. traditional food/item orders). Null for stay/ticket-style bookings. */
    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    /** Contact phone the vendor should use to confirm/coordinate an order. Null for stay/ticket-style bookings. */
    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 10)
    private CancelledBy cancelledBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}


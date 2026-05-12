package aptms.entities;

import aptms.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * VendorService entity representing a bookable service listing.
 * Requirements: BRD §6.2 — vendor_service table
 */
@Entity
@Table(name = "vendor_service")
@Data
public class VendorService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 30)
    private ServiceType serviceType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_unit", nullable = false, length = 20)
    private PricingUnit pricingUnit;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "min_booking_notice")
    private Integer minBookingNotice;

    @Column(name = "max_booking_advance")
    private Integer maxBookingAdvance;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_mode", nullable = false, length = 10)
    private BookingMode bookingMode = BookingMode.MANUAL;

    @Column(name = "confirmation_window")
    private Integer confirmationWindow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ServiceStatus status = ServiceStatus.DRAFT;

    @Column(name = "cancellation_policy", columnDefinition = "TEXT")
    private String cancellationPolicy;

    @Column(name = "location_lat", precision = 10, scale = 8)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 11, scale = 8)
    private BigDecimal locationLng;

    @Column(name = "location_address", length = 500)
    private String locationAddress;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "total_bookings")
    private Integer totalBookings = 0;

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}


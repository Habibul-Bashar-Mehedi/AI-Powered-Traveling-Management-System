package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

/**
 * One real, bookable component of a Package — always a VendorService row
 * (HOTEL_ROOM / TRANSPORT_ROUTE / TOUR_PACKAGE). Booking the package reserves
 * real inventory on each component's VendorService via the same pessimistic-lock
 * path used for standalone service bookings.
 */
@Entity
@Table(name = "package_component")
@Data
public class PackageComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID componentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Package packageEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private VendorService service;

    /** Units of the linked service to reserve per package booking. */
    @Column(nullable = false)
    private Integer quantity = 1;

    /** Which day of the trip this component happens (1-based). Null = whole trip. */
    @Column(name = "day_number")
    private Integer dayNumber;

    /** Display order. */
    @Column(nullable = false)
    private Integer sequence = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;
}

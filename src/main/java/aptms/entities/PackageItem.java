package aptms.entities;

import aptms.enums.PackageItemType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID;

/**
 * One leg/component of a TOUR_PACKAGE listing's itinerary — e.g. "outbound transport",
 * "hotel stay", "return transport". Purely descriptive: the parent VendorService is still
 * the single bookable/priced/capacity-tracked unit, this just breaks down what it bundles.
 */
@Entity
@Table(name = "package_item")
@Data
public class PackageItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VendorService service;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private PackageItemType itemType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Which day of the itinerary this falls on (1-based). Null = unspecified/whole trip. */
    @Column(name = "day_number")
    private Integer dayNumber;

    /** Display order within the same day. */
    @Column(nullable = false)
    private Integer sequence = 0;
}

package aptms.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.UUID;

@Entity
@Audited
@Data
@Table(name = "traditionalItems")
public class TraditionalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne
    @JoinColumn(name = "market_id", referencedColumnName = "id")
    private Market market;

    /**
     * Optional direct link for geolocation filtering. Existing rows only have a
     * market (and reach a destination via market.getDestination()) — this is not
     * backfilled; callers fall back to the market's destination when this is null.
     */
    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destination;

    private String categoryName;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String priceRange;
    private boolean isDeleted;

    /**
     * The real, bookable/capacity-tracked VendorService this item maps to
     * (set by an admin once it's actually orderable). Never serialized directly —
     * VendorService.vendor carries sensitive wallet/commission fields — see getLinkedServiceId().
     * Mirrors TraditionalFood.linkedService.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_service_id")
    @JsonIgnore
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private VendorService linkedService;

    /** Safe, flat id for the frontend to drive availability/ordering via the existing service-catalog endpoints. */
    public UUID getLinkedServiceId() {
        return linkedService != null ? linkedService.getServiceId() : null;
    }
}

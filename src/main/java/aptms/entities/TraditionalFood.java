package aptms.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.Date;
import java.util.UUID;

@Entity
@Audited
@Data
@Table(name = "traditional_foods")
public class TraditionalFood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destination;
    private String dishName;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String culturalContext;
    private String priceRange;
    private String recommendedLocation;
    private boolean isDeleted;

    /**
     * The real, bookable/capacity-tracked VendorService this dish experience maps to
     * (set by an admin once it's actually bookable). Never serialized directly —
     * VendorService.vendor carries sensitive wallet/commission fields — see getLinkedServiceId().
     * Presence of a linked service is itself the "bookable" signal for this entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_service_id")
    @JsonIgnore
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private VendorService linkedService;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

    /** Safe, flat id for the frontend to drive availability/booking via the existing service-catalog endpoints. */
    public UUID getLinkedServiceId() {
        return linkedService != null ? linkedService.getServiceId() : null;
    }
}

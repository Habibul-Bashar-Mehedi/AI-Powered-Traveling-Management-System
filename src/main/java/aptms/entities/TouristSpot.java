package aptms.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.util.Date;
import java.util.UUID;

@Entity
@Audited
@Data
@Table(name = "tourist_spots")
public class TouristSpot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destination;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String visitingHours;
    private double adultEntryFees;
    private double childEntryFees;
    private String locationDescription;
    private boolean isAlive = true;
    private boolean isDelete;

    /** Paid vs. free — drives "Buy Ticket" vs. "Visit" info-only in the UI. */
    private boolean requiresTicket = false;

    /**
     * The real, bookable/capacity-tracked VendorService this spot's ticket maps to
     * (set by an admin once the spot is actually ticketed). Never serialized directly —
     * VendorService.vendor carries sensitive wallet/commission fields — see getLinkedServiceId().
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_service_id")
    @JsonIgnore
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private VendorService linkedService;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
    @UpdateTimestamp
    private Date deletedAt;

    /** Safe, flat id for the frontend to drive availability/booking via the existing service-catalog endpoints. */
    public UUID getLinkedServiceId() {
        return linkedService != null ? linkedService.getServiceId() : null;
    }
}

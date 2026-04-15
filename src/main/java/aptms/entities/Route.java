package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Entity
@Audited
@Data
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "origin_id",referencedColumnName = "id")
    private Destination origin;
    @ManyToOne
    @JoinColumn(name = "destination_id",referencedColumnName = "id")
    private Destination destination;
    private double distanceKM;
    private String estimatedDuration;
    @Column(columnDefinition = "TEXT")
    private String routeDescription;
    private boolean isRecommended;
    private boolean isDeleted;
    @CreationTimestamp
    @Column(updatable = false)
    private Date CreatedAt;
}

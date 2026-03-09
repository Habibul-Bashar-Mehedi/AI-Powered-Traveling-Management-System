package ai_powered_traveling_management_system.route.entities;

import ai_powered_traveling_management_system.destination.entities.Destination;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
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

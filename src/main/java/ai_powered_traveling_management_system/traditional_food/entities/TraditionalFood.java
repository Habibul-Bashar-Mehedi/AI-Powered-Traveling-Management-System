package ai_powered_traveling_management_system.traditional_food.entities;

import ai_powered_traveling_management_system.destination.entities.Destination;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "traditional_foods")
public class TraditionalFood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
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
    private Date createdAt;

}

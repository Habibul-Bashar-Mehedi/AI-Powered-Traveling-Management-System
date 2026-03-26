package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

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
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

}

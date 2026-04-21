package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Entity
@Audited
@Data
@Table(name = "Markets")
public class Market {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destination;
    private String name;
    private String location;
    private String operatingDays;
    private String operatingHours;
    @Column(columnDefinition = "TEXT")
    private String description;
    private boolean isAlive;
    private boolean isDeleted;
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

}

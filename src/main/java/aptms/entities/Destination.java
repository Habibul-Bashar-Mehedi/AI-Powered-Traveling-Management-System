package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Entity
@Audited
@Data
@Table(name = "destination")
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Integer version;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 100)
    private String region;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private boolean isAlive = true;

    private Double latitude;

    private Double longitude;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
}

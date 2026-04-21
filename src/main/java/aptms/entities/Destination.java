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
    private String name;
    private String region;
    @Column(columnDefinition = "TEXT")
    private String description;
    private boolean isAlive = true;
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
}

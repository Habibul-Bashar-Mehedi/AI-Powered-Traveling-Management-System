package ai_powered_traveling_management_system.destination.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;

@Entity
@Data
@Table(name = "destination")
public class Destination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String region;
    @Column(columnDefinition = "TEXT")
    private String description;
    private boolean isAlive = true;
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
}

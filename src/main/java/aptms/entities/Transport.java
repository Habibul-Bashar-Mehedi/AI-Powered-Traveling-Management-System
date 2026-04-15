package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.util.Date;

@Entity
@Audited
@Data
@Table(name = "transports")
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "origin_id", referencedColumnName = "id")
    private Destination origin;
    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destination;
    private String model;
    private String operatorName;
    private double estimatedCost;
    private String estimatedDuration;
    private String frequency;
    private boolean isLocal;
    private Date createdAt;
}

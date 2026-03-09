package ai_powered_traveling_management_system.transport.entities;

import ai_powered_traveling_management_system.destination.entities.Destination;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
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

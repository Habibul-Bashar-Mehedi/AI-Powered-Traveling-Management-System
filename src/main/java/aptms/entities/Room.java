package aptms.entities;

import aptms.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Table(name = "rooms")
@Audited
@Entity
@Data
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Integer version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", referencedColumnName = "id", nullable = false)
    private Hotel hotel;
    
    @Column(nullable = false, length = 50)
    private String roomTypeName;
    
    @Column(columnDefinition = "TEXT")
    private String amenities;
    
    @Column(nullable = false)
    private double pricePerNight;
    
    @Column(nullable = false)
    private int availableQuantities;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;
    
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
    
    @UpdateTimestamp
    private Date updatedAt;
}

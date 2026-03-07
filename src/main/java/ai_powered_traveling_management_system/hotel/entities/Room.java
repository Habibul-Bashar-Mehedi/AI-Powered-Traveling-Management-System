package ai_powered_traveling_management_system.hotel.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Table(name = "rooms")
@Entity
@Data
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "hotel_id", referencedColumnName = "id")
    private Hotel hotel; // 'hotelId' er bodole 'hotel' likhun

    private String roomTypeName;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    private double pricePerNight;
    private int availableQuantities;

    @Column(columnDefinition = "TEXT")
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

    @UpdateTimestamp // Eta fix kora holo, jate update korle automatic time change hoy
    private Date updatedAt;

}

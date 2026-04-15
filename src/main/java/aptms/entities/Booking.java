package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Table(name= "booking")
@Entity
@Audited
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "user_id",referencedColumnName = "id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "room_id", referencedColumnName = "id")
    private Room room;
    @ManyToOne
    @JoinColumn(name = "hotel_id", referencedColumnName = "id")
    private Hotel hotel;
    private Date checkInDate;
    private Date checkOutDate;
    private int guestCount;
    private Double totalPrice;
    @Column(columnDefinition = "TEXT")
    private String status;
    @Column(columnDefinition = "TEXT")
    private String specialRequest;
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
    @CreationTimestamp
    private Date updatedAt;


}

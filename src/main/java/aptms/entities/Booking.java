package aptms.entities;

import aptms.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Table(name= "booking")
@Entity
@Audited
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Integer version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "id", nullable = false)
    private Room room;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", referencedColumnName = "id", nullable = false)
    private Hotel hotel;
    
    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date checkInDate;
    
    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date checkOutDate;
    
    @Column(nullable = false)
    private int guestCount;
    
    @Column(nullable = false)
    private Double totalPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String specialRequest;
    
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
    
    @UpdateTimestamp
    private Date updatedAt;
}

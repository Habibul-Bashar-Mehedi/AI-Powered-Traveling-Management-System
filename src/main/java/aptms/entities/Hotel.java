package aptms.entities;

import aptms.enums.HotelStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import java.util.Date;

@Table(name = "hotels")
@Audited
@Entity
@Data
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version
    private Integer version;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", referencedColumnName = "id")
    private User vendor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destination;
    
    @Column(nullable = false, length = 100)
    private String hotelName;
    
    @Column(nullable = false, length = 255)
    private String address;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HotelStatus status = HotelStatus.ACTIVE;
    
    @Column(columnDefinition = "TEXT")
    private String descriptions;
    
    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
    
    @UpdateTimestamp
    private Date updatedAt;
    
    @Column(nullable = false)
    private boolean isDeleted = false;
}

package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;


@Table(name = "hotels")
@Entity
@Data
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Foreign Key Mapping
    @ManyToOne(fetch = FetchType.LAZY) // Onek gulo Hotel ekta Vendor-er hote pare
    @JoinColumn(name = "vendor_id", referencedColumnName = "id") // Database-e 'vendor_id' name FK hobe
    private User vendor;
    @ManyToOne
    @JoinColumn(name = "destination_id", referencedColumnName = "id")
    private Destination destinationId;

    private String hotelName;
    private String address;
    private String status;
    @Column(columnDefinition = "TEXT")//description beshi hote pare dekhe use kora hoiyese
    private String descriptions;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;
    @UpdateTimestamp
    private Date updatedAt;
    private boolean isDeleted;
}

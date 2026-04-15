package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

@Entity
@Audited
@Data
@Table(name = "traditionalItems")
public class TraditionalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "market_id", referencedColumnName = "id")
    private Market market;
    private String categoryName;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String priceRange;
    private boolean isDeleted;
}

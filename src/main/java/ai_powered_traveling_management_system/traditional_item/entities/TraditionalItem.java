package ai_powered_traveling_management_system.traditional_item.entities;

import ai_powered_traveling_management_system.market.entities.Market;
import jakarta.persistence.*;
import lombok.Data;

@Entity
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

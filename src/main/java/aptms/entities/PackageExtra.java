package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Optional add-on line shown alongside a Package. Purely descriptive/pricing —
 * carries no inventory or capacity, same convention as PackageItem.
 */
@Entity
@Table(name = "package_extra")
@Data
public class PackageExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID extraId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Package packageEntity;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** Whether this is baked into the package's totalPrice, vs. an optional paid add-on. */
    @Column(nullable = false)
    private Boolean included = true;
}

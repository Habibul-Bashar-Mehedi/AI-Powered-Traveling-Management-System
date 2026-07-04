package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin-managed promotional banner shown on the user dashboard.
 */
@Entity
@Table(name = "banner")
@Data
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "badge_text", length = 40)
    private String badgeText;

    @Column(name = "cta_label", length = 40)
    private String ctaLabel = "Explore";

    /**
     * Either a dashboard section id to scroll to (e.g. "offers", "services", "destinations")
     * or a full "http(s)://" URL to open in a new tab.
     */
    @Column(name = "cta_target", length = 200)
    private String ctaTarget = "offers";

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

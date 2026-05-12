package aptms.entities;

import aptms.enums.PayoutMethod;
import aptms.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * PayoutRequest entity — vendor payout request lifecycle per BRD FR-WAL-003.
 */
@Entity
@Table(name = "payout_request")
@Data
public class PayoutRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID payoutId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", nullable = false, length = 20)
    private PayoutMethod payoutMethod;

    @Column(name = "payout_details", columnDefinition = "TEXT")
    private String payoutDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}


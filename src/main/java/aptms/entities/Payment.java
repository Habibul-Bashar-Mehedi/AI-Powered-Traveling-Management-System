package aptms.entities;

import aptms.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A single SSLCommerz checkout session/transaction, linked to exactly one of
 * VendorBooking or PackageBooking (never both) — the real gateway-facing ledger
 * row, distinct from WalletTransaction (which is the vendor payout-side ledger).
 */
@Entity
@Table(name = "payment")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID paymentId;

    @Column(name = "tx_id", nullable = false, unique = true, length = 64)
    private String txId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_booking_id")
    private VendorBooking vendorBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_booking_id")
    private PackageBooking packageBooking;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "BDT";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Raw method SSLCommerz reports (e.g. "bKash", "Visa") — not our internal PaymentMethod enum. */
    @Column(name = "gateway_card_type", length = 100)
    private String gatewayCardType;

    /** SSLCommerz's val_id — required to re-run the validation call idempotently. */
    @Column(name = "gateway_val_id", length = 100)
    private String gatewayValId;

    /** Raw last validation/IPN payload, kept for audit/debugging. */
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @CreationTimestamp
    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

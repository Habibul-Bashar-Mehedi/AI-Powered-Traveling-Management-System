package aptms.dto.vendor;

import aptms.enums.CancelledBy;
import aptms.enums.PaymentMethod;
import aptms.enums.VendorBookingStatus;
import aptms.enums.VendorPaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for vendor booking inbox and detail view.
 * Requirements: BRD FR-BKG-001, FR-BKG-004
 */
@Data
public class VendorBookingDTO {

    private UUID bookingId;
    private UUID serviceId;
    private String serviceName;
    private UUID vendorId;
    private String vendorBusinessName;
    private UUID userId;
    private String userName;
    private String userEmail;
    private VendorBookingStatus bookingStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private BigDecimal grossAmount;
    private BigDecimal commissionAmount;
    private BigDecimal netAmount;
    private VendorPaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private String specialRequests;
    private String cancellationReason;
    private CancelledBy cancelledBy;
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant completedAt;

    // For reject/cancel actions
    private String reason;
}


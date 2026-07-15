package aptms.dto;

import aptms.enums.PaymentBookingType;
import aptms.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentStatusDTO {
    private UUID paymentId;
    private String txId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currencyCode;
    private String gatewayCardType;
    private PaymentBookingType bookingType;
    private UUID bookingId;
    private String bookingReference;
    private Instant initiatedAt;
    private Instant updatedAt;
}

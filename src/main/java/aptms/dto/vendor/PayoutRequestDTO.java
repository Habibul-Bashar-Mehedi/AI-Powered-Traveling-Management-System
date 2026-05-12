package aptms.dto.vendor;

import aptms.enums.PayoutMethod;
import aptms.enums.PayoutStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for payout requests.
 * Requirements: BRD FR-WAL-003, FR-WAL-004
 */
@Data
public class PayoutRequestDTO {

    private UUID payoutId;

    @NotNull
    @DecimalMin("50.00")
    private BigDecimal amount;

    @NotNull
    private PayoutMethod payoutMethod;

    private String payoutDetails;

    // Response fields
    private PayoutStatus status;
    private String adminNote;
    private Instant requestedAt;
    private Instant processedAt;
}


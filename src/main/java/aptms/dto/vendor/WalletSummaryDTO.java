package aptms.dto.vendor;

import aptms.enums.PayoutMethod;
import aptms.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for vendor wallet summary — balance, transactions, payout methods.
 * Requirements: BRD FR-WAL-002, FR-WAL-006
 */
@Data
public class WalletSummaryDTO {

    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal lifetimeEarnings;
    private PayoutMethod payoutMethod;
    private List<TransactionDTO> recentTransactions;

    @Data
    public static class TransactionDTO {
        private UUID transactionId;
        private TransactionType transactionType;
        private BigDecimal amount;
        private BigDecimal balanceAfter;
        private String description;
        private Instant createdAt;
        private UUID bookingId;
    }
}


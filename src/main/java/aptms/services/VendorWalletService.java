package aptms.services;

import aptms.dto.vendor.PayoutRequestDTO;
import aptms.dto.vendor.WalletSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for vendor wallet and payout operations.
 */
public interface VendorWalletService {

    WalletSummaryDTO getWalletSummary(UUID userId);

    Page<WalletSummaryDTO.TransactionDTO> getTransactionHistory(UUID userId, Pageable pageable);

    PayoutRequestDTO requestPayout(UUID userId, PayoutRequestDTO request);
}


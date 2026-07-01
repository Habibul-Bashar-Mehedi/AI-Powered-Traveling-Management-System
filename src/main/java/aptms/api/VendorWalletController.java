package aptms.api;

import aptms.dto.vendor.PayoutRequestDTO;
import aptms.dto.vendor.WalletSummaryDTO;
import aptms.security.SecurityUtils;
import aptms.services.VendorWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for vendor wallet and payout operations.
 * Requirements: BRD §4.4 — Earnings & Wallet
 */
@RestController
@RequestMapping("/api/v1/vendor/wallet")
@PreAuthorize("hasRole('VENDOR')")
@RequiredArgsConstructor
@Tag(name = "Vendor Wallet", description = "Earnings, wallet balance, transactions and payout management")
public class VendorWalletController {

    private final VendorWalletService walletService;

    @GetMapping
    @Operation(summary = "Get wallet summary (FR-WAL-002)")
    public ResponseEntity<WalletSummaryDTO> getWalletSummary() {
        return ResponseEntity.ok(walletService.getWalletSummary(getCurrentUserId()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get paginated transaction history (FR-WAL-006)")
    public ResponseEntity<Page<WalletSummaryDTO.TransactionDTO>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                walletService.getTransactionHistory(getCurrentUserId(), PageRequest.of(page, size)));
    }

    @PostMapping("/payout")
    @Operation(summary = "Request a payout (FR-WAL-003)")
    public ResponseEntity<PayoutRequestDTO> requestPayout(@Valid @RequestBody PayoutRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(walletService.requestPayout(getCurrentUserId(), request));
    }

    private UUID getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}


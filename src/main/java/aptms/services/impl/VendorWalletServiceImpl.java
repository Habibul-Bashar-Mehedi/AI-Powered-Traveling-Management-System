package aptms.services.impl;

import aptms.dto.vendor.PayoutRequestDTO;
import aptms.dto.vendor.WalletSummaryDTO;
import aptms.entities.PayoutRequest;
import aptms.entities.Vendor;
import aptms.entities.WalletTransaction;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.PayoutRequestRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.WalletTransactionRepository;
import aptms.services.VendorWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorWalletServiceImpl implements VendorWalletService {

    private final VendorRepository vendorRepository;
    private final WalletTransactionRepository transactionRepository;
    private final PayoutRequestRepository payoutRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryDTO getWalletSummary(UUID userId) {
        Vendor vendor = getVendorByUserId(userId);

        WalletSummaryDTO dto = new WalletSummaryDTO();
        dto.setAvailableBalance(vendor.getWalletBalance());
        dto.setPendingBalance(vendor.getPendingBalance());
        dto.setPayoutMethod(vendor.getPayoutMethod());

        // Compute lifetime earnings from transactions
        BigDecimal lifetime = transactionRepository
                .findByVendorVendorIdOrderByCreatedAtDesc(vendor.getVendorId(), Pageable.unpaged())
                .stream()
                .filter(t -> t.getTransactionType() == aptms.enums.TransactionType.CREDIT)
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setLifetimeEarnings(lifetime);

        // Recent 10 transactions
        var recent = transactionRepository
                .findByVendorVendorIdOrderByCreatedAtDesc(vendor.getVendorId(),
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .stream()
                .map(this::toTransactionDTO)
                .collect(Collectors.toList());
        dto.setRecentTransactions(recent);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletSummaryDTO.TransactionDTO> getTransactionHistory(UUID userId, Pageable pageable) {
        Vendor vendor = getVendorByUserId(userId);
        return transactionRepository
                .findByVendorVendorIdOrderByCreatedAtDesc(vendor.getVendorId(), pageable)
                .map(this::toTransactionDTO);
    }

    @Override
    @Transactional
    public PayoutRequestDTO requestPayout(UUID userId, PayoutRequestDTO request) {
        Vendor vendor = getVendorByUserId(userId);

        if (vendor.getWalletBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient wallet balance for payout");
        }
        if (request.getAmount().compareTo(new BigDecimal("50.00")) < 0) {
            throw new IllegalStateException("Minimum payout amount is $50.00");
        }

        PayoutRequest entity = new PayoutRequest();
        entity.setVendor(vendor);
        entity.setAmount(request.getAmount());
        entity.setPayoutMethod(request.getPayoutMethod() != null ? request.getPayoutMethod() : vendor.getPayoutMethod());
        entity.setPayoutDetails(request.getPayoutDetails());

        PayoutRequest saved = payoutRequestRepository.save(entity);
        log.info("Payout request {} created for vendor {}", saved.getPayoutId(), vendor.getVendorId());
        return toPayoutDTO(saved);
    }

    private Vendor getVendorByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IdNotFoundException("Vendor profile not found for user: " + userId));
    }

    private WalletSummaryDTO.TransactionDTO toTransactionDTO(WalletTransaction t) {
        WalletSummaryDTO.TransactionDTO dto = new WalletSummaryDTO.TransactionDTO();
        dto.setTransactionId(t.getTransactionId());
        dto.setTransactionType(t.getTransactionType());
        dto.setAmount(t.getAmount());
        dto.setBalanceAfter(t.getBalanceAfter());
        dto.setDescription(t.getDescription());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setBookingId(t.getBooking() != null ? t.getBooking().getBookingId() : null);
        return dto;
    }

    private PayoutRequestDTO toPayoutDTO(PayoutRequest p) {
        PayoutRequestDTO dto = new PayoutRequestDTO();
        dto.setPayoutId(p.getPayoutId());
        dto.setAmount(p.getAmount());
        dto.setPayoutMethod(p.getPayoutMethod());
        dto.setPayoutDetails(p.getPayoutDetails());
        dto.setStatus(p.getStatus());
        dto.setAdminNote(p.getAdminNote());
        dto.setRequestedAt(p.getRequestedAt());
        dto.setProcessedAt(p.getProcessedAt());
        return dto;
    }
}


package aptms.services.impl;

import aptms.config.CacheConfig;
import aptms.dto.vendor.AdminVendorUpdateDTO;
import aptms.dto.vendor.PayoutRequestDTO;
import aptms.dto.vendor.VendorProfileDTO;
import aptms.entities.PayoutRequest;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.entities.WalletTransaction;
import aptms.enums.PayoutStatus;
import aptms.enums.TransactionType;
import aptms.enums.VendorStatus;
import aptms.repositories.PayoutRequestRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.WalletTransactionRepository;
import aptms.services.AdminVendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminVendorServiceImpl implements AdminVendorService {

    private static final int MIN_SUSPENSION_REASON_LENGTH = 10;

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_VENDORS_PENDING)
    public List<VendorProfileDTO> getPendingVendors() {
        return vendorRepository.searchByStatuses(
                        List.of(VendorStatus.PENDING, VendorStatus.PENDING_REVIEW),
                        null,
                        org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .map(VendorRegistrationServiceImpl::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(CacheConfig.CACHE_VENDORS_ALL)
    public List<VendorProfileDTO> getAllVendors() {
        return vendorRepository.findAll()
                .stream().map(VendorRegistrationServiceImpl::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_PENDING, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_ALL,     allEntries = true)
    })
    public VendorProfileDTO approveVendor(UUID vendorId, UUID adminUserId) {
        Vendor vendor = getVendor(vendorId);
        User admin = getUser(adminUserId);
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setApprovedAt(Instant.now());
        vendor.setApprovedBy(admin);
        vendor.setRejectionReason(null);
        log.info("Vendor {} approved by admin {}", vendorId, adminUserId);
        return VendorRegistrationServiceImpl.toDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_PENDING, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_ALL,     allEntries = true)
    })
    public VendorProfileDTO rejectVendor(UUID vendorId, UUID adminUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        Vendor vendor = getVendor(vendorId);
        vendor.setStatus(VendorStatus.REJECTED);
        vendor.setRejectionReason(reason);
        log.info("Vendor {} rejected by admin {}", vendorId, adminUserId);
        return VendorRegistrationServiceImpl.toDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_PENDING, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_ALL,     allEntries = true)
    })
    public VendorProfileDTO suspendVendor(UUID vendorId, UUID adminUserId, String reason) {
        if (reason == null || reason.trim().length() < MIN_SUSPENSION_REASON_LENGTH) {
            throw new IllegalArgumentException(
                    "Suspension reason must be at least " + MIN_SUSPENSION_REASON_LENGTH + " characters");
        }
        Vendor vendor = getVendor(vendorId);
        User admin = getUser(adminUserId);
        vendor.setStatus(VendorStatus.SUSPENDED);
        vendor.setSuspensionReason(reason.trim());
        vendor.setSuspendedAt(Instant.now());
        vendor.setSuspendedBy(admin);
        log.info("Vendor {} suspended by admin {}", vendorId, adminUserId);
        return VendorRegistrationServiceImpl.toDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_PENDING, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_ALL,     allEntries = true)
    })
    public VendorProfileDTO updateVendor(UUID vendorId, UUID adminUserId, AdminVendorUpdateDTO dto) {
        Vendor vendor = getVendor(vendorId);
        if (dto.getVendorType() != null) {
            vendor.setVendorType(dto.getVendorType());
        }
        if (dto.getBusinessName() != null && !dto.getBusinessName().isBlank()) {
            vendor.setBusinessName(dto.getBusinessName().trim());
        }
        log.info("Vendor {} updated by admin {}", vendorId, adminUserId);
        return VendorRegistrationServiceImpl.toDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_PENDING, allEntries = true),
        @CacheEvict(value = CacheConfig.CACHE_VENDORS_ALL,     allEntries = true)
    })
    public VendorProfileDTO reinstateVendor(UUID vendorId, UUID adminUserId) {
        Vendor vendor = getVendor(vendorId);
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setRejectionReason(null);
        vendor.setSuspensionReason(null);
        vendor.setSuspendedAt(null);
        vendor.setSuspendedBy(null);
        log.info("Vendor {} reinstated by admin {}", vendorId, adminUserId);
        return VendorRegistrationServiceImpl.toDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayoutRequestDTO> getPendingPayouts() {
        return payoutRequestRepository.findByStatus(PayoutStatus.PENDING)
                .stream().map(this::toPayoutDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PayoutRequestDTO processePayout(UUID payoutId, UUID adminUserId, boolean approve, String note) {
        PayoutRequest request = payoutRequestRepository.findById(payoutId)
                .orElseThrow(() -> new IllegalArgumentException("Payout request not found: " + payoutId));
        User admin = getUser(adminUserId);

        request.setProcessedBy(admin);
        request.setProcessedAt(Instant.now());
        request.setAdminNote(note);

        if (approve) {
            Vendor vendor = request.getVendor();

            // Re-validate balance at approval time, not just at request time — it may have
            // moved (e.g. another payout already approved) between the two.
            if (vendor.getWalletBalance().compareTo(request.getAmount()) < 0) {
                request.setStatus(PayoutStatus.FAILED);
                request.setAdminNote((note == null ? "" : note + " — ")
                        + "Auto-failed: vendor wallet balance is now insufficient.");
                return toPayoutDTO(payoutRequestRepository.save(request));
            }

            // No real disbursement API exists for SSLCommerz (it's a collection gateway,
            // not a payout/mass-transfer service) — this is a clearly-labeled realistic
            // simulation: PROCESSING marks the transfer as "in flight" before settling to
            // COMPLETED, exactly mirroring how a real bank/mobile-wallet payout would report
            // an intermediate state.
            request.setStatus(PayoutStatus.PROCESSING);

            vendor.setWalletBalance(vendor.getWalletBalance().subtract(request.getAmount()));
            vendorRepository.save(vendor);

            WalletTransaction debit = new WalletTransaction();
            debit.setVendor(vendor);
            debit.setTransactionType(TransactionType.DEBIT);
            debit.setAmount(request.getAmount());
            debit.setBalanceAfter(vendor.getWalletBalance());
            debit.setDescription("Payout " + request.getPayoutId() + " (" + request.getPayoutMethod() + ", simulated transfer)");
            walletTransactionRepository.save(debit);

            String simulatedTransferRef = "SIM-PAYOUT-" + request.getPayoutId().toString().substring(0, 8).toUpperCase();
            request.setAdminNote((note == null ? "" : note + " — ")
                    + "Simulated transfer ref " + simulatedTransferRef + " (no live disbursement API available).");
            request.setStatus(PayoutStatus.COMPLETED);
        } else {
            request.setStatus(PayoutStatus.FAILED);
        }

        return toPayoutDTO(payoutRequestRepository.save(request));
    }

    private Vendor getVendor(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorId));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
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


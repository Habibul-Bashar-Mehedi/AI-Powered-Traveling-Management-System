package aptms.services.impl;

import aptms.dto.vendor.AdminVendorActionDTO;
import aptms.dto.vendor.PayoutRequestDTO;
import aptms.dto.vendor.VendorProfileDTO;
import aptms.entities.PayoutRequest;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.PayoutStatus;
import aptms.enums.VendorStatus;
import aptms.repositories.PayoutRequestRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.services.AdminVendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final PayoutRequestRepository payoutRequestRepository;

    @Override
    @Transactional(readOnly = true)
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
    public List<VendorProfileDTO> getAllVendors() {
        return vendorRepository.findAll()
                .stream().map(VendorRegistrationServiceImpl::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
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
    public VendorProfileDTO suspendVendor(UUID vendorId, UUID adminUserId, String reason) {
        Vendor vendor = getVendor(vendorId);
        vendor.setStatus(VendorStatus.SUSPENDED);
        vendor.setRejectionReason(reason);
        log.info("Vendor {} suspended by admin {}", vendorId, adminUserId);
        return VendorRegistrationServiceImpl.toDTO(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public VendorProfileDTO reinstateVendor(UUID vendorId, UUID adminUserId) {
        Vendor vendor = getVendor(vendorId);
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setRejectionReason(null);
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
            request.setStatus(PayoutStatus.COMPLETED);
            // Deduct from vendor wallet
            Vendor vendor = request.getVendor();
            vendor.setWalletBalance(vendor.getWalletBalance().subtract(request.getAmount()));
            vendorRepository.save(vendor);
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


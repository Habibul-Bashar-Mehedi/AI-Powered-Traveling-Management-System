package aptms.services.impl;

import aptms.dto.vendor.ReinstatementRequestDTO;
import aptms.entities.ReinstatementRequest;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.ReinstatementStatus;
import aptms.enums.VendorStatus;
import aptms.repositories.ReinstatementRequestRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.services.AdminVendorService;
import aptms.services.ReinstatementRequestService;
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
public class ReinstatementRequestServiceImpl implements ReinstatementRequestService {

    private final ReinstatementRequestRepository reinstatementRequestRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final AdminVendorService adminVendorService;

    @Override
    @Transactional
    public ReinstatementRequestDTO createRequest(UUID userId, String message) {
        Vendor vendor = getVendorByUserId(userId);

        if (vendor.getStatus() != VendorStatus.SUSPENDED) {
            throw new IllegalStateException("Only suspended vendor accounts can request reinstatement");
        }
        if (reinstatementRequestRepository.existsByVendorVendorIdAndStatus(
                vendor.getVendorId(), ReinstatementStatus.PENDING)) {
            throw new IllegalStateException("A reinstatement request is already pending review");
        }

        ReinstatementRequest request = new ReinstatementRequest();
        request.setVendor(vendor);
        request.setMessage(message);
        request.setStatus(ReinstatementStatus.PENDING);

        log.info("Reinstatement request created for vendor {}", vendor.getVendorId());
        return toDTO(reinstatementRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReinstatementRequestDTO> getMyRequests(UUID userId) {
        Vendor vendor = getVendorByUserId(userId);
        return reinstatementRequestRepository.findByVendorVendorIdOrderBySubmittedAtDesc(vendor.getVendorId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReinstatementRequestDTO> getAllRequests(String status) {
        List<ReinstatementRequest> requests = status == null
                ? reinstatementRequestRepository.findAllWithDetailsOrderBySubmittedAtDesc()
                : reinstatementRequestRepository.findByStatusWithDetailsOrderBySubmittedAtDesc(
                        ReinstatementStatus.valueOf(status));
        return requests.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReinstatementRequestDTO reviewRequest(UUID requestId, UUID adminUserId, boolean approve, String rejectionReason) {
        ReinstatementRequest request = reinstatementRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Reinstatement request not found: " + requestId));

        if (request.getStatus() != ReinstatementStatus.PENDING) {
            throw new IllegalStateException("Request has already been reviewed");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + adminUserId));

        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);

        if (approve) {
            request.setStatus(ReinstatementStatus.APPROVED);
            adminVendorService.reinstateVendor(request.getVendor().getVendorId(), adminUserId);
            log.info("Reinstatement request {} approved by admin {}", requestId, adminUserId);
        } else {
            request.setStatus(ReinstatementStatus.REJECTED);
            request.setRejectionReason(rejectionReason);
            log.info("Reinstatement request {} rejected by admin {}", requestId, adminUserId);
        }

        return toDTO(reinstatementRequestRepository.save(request));
    }

    private Vendor getVendorByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found for user: " + userId));
    }

    private ReinstatementRequestDTO toDTO(ReinstatementRequest r) {
        ReinstatementRequestDTO dto = new ReinstatementRequestDTO();
        dto.setRequestId(r.getRequestId());
        dto.setVendorId(r.getVendor() != null ? r.getVendor().getVendorId() : null);
        dto.setVendorBusinessName(r.getVendor() != null ? r.getVendor().getBusinessName() : null);
        dto.setMessage(r.getMessage());
        dto.setStatus(r.getStatus());
        dto.setRejectionReason(r.getRejectionReason());
        dto.setSubmittedAt(r.getSubmittedAt());
        dto.setReviewedAt(r.getReviewedAt());
        dto.setReviewedByName(r.getReviewedBy() != null ? r.getReviewedBy().getUsername() : null);
        return dto;
    }
}

package aptms.services;

import aptms.dto.vendor.ReinstatementRequestDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service for a suspended vendor's reinstatement request lifecycle and its admin review.
 */
public interface ReinstatementRequestService {

    /** Vendor-facing: create a new reinstatement request for the calling user's vendor account. */
    ReinstatementRequestDTO createRequest(UUID userId, String message);

    /** Vendor-facing: list this vendor's own reinstatement requests. */
    List<ReinstatementRequestDTO> getMyRequests(UUID userId);

    /** Admin-facing: list all reinstatement requests, optionally filtered by status. */
    List<ReinstatementRequestDTO> getAllRequests(String status);

    /** Admin-facing: approve or reject a pending request. */
    ReinstatementRequestDTO reviewRequest(UUID requestId, UUID adminUserId, boolean approve, String rejectionReason);
}

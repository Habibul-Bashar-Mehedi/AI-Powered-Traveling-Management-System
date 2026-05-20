package aptms.dto.admin;

import aptms.enums.VendorStatus;
import aptms.enums.VendorType;

import java.time.Instant;
import java.util.UUID;

public record VendorSummaryResponse(
        UUID vendorId,
        UUID userId,
        String businessName,
        String email,
        String phone,
        VendorType vendorType,
        VendorStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant approvedAt,
        UUID approvedBy
) {
}


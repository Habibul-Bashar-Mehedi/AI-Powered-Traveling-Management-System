package aptms.dto.vendor;

import aptms.enums.ReinstatementStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ReinstatementRequestDTO {
    private UUID requestId;
    private UUID vendorId;
    private String vendorBusinessName;
    private String message;
    private ReinstatementStatus status;
    private String rejectionReason;
    private Instant submittedAt;
    private Instant reviewedAt;
    private String reviewedByName;
}

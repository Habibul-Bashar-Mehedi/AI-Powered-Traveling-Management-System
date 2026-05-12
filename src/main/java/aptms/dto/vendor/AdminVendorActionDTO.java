package aptms.dto.vendor;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for admin actions on vendor accounts (approve/reject/suspend).
 * Requirements: BRD FR-REG-005
 */
@Data
public class AdminVendorActionDTO {

    private String reason;

    @NotBlank
    private String action; // APPROVE, REJECT, SUSPEND, REINSTATE
}


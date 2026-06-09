package aptms.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendorDecisionRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}

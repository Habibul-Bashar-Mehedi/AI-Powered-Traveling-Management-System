package aptms.dto.admin;

import jakarta.validation.constraints.Size;

public record VendorSuspendRequest(
        @Size(max = 1000) String reason
) {
}


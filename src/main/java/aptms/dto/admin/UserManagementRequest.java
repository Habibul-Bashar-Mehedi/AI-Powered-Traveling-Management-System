package aptms.dto.admin;

import aptms.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserManagementRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(max = 100) String password,
        @NotNull UserRole role,
        @Size(max = 100) String countryId,
        @Size(max = 20) String vendorType
) {
}


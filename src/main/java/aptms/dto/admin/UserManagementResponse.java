package aptms.dto.admin;

import aptms.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserManagementResponse(
        UUID id,
        String username,
        String email,
        UserRole role,
        String countryId,
        Integer failedLoginAttempts,
        Instant lockoutUntil,
        Instant createdAt,
        Instant updatedAt
) {
}


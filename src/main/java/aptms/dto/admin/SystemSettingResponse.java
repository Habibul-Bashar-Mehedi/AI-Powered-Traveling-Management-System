package aptms.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record SystemSettingResponse(
        UUID settingId,
        String settingKey,
        String settingValue,
        String description,
        String category,
        Boolean active,
        UUID updatedByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}


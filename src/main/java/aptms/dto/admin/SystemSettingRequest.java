package aptms.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SystemSettingRequest(
        @NotBlank @Size(max = 120) String settingKey,
        @NotBlank @Size(max = 5000) String settingValue,
        @Size(max = 500) String description,
        @Size(max = 80) String category,
        @NotNull Boolean active
) {
}

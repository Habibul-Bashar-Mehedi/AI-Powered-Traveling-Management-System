package aptms.dto.admin;

import aptms.enums.AdminOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record OrderRequest(
        @NotNull UUID userId,
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity,
        @NotNull AdminOrderStatus status
) {
}

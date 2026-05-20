package aptms.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 80) String sku,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        @NotNull @PositiveOrZero Integer stockQuantity,
        @NotNull Boolean active
) {
}


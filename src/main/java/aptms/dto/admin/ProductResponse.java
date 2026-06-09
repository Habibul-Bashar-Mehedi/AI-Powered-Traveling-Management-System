package aptms.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String name,
        String sku,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

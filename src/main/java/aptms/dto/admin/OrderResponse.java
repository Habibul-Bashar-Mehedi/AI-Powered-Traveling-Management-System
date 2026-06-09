package aptms.dto.admin;

import aptms.enums.AdminOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String orderNumber,
        UUID userId,
        String userEmail,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        AdminOrderStatus status,
        Instant placedAt,
        Instant updatedAt
) {
}

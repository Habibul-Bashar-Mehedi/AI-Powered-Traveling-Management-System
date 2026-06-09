package aptms.dto.admin;

import java.math.BigDecimal;

public record AdminDashboardAnalyticsResponse(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        BigDecimal totalRevenue,
        long pendingOrders,
        long completedOrders,
        long totalVendors,
        long pendingVendors,
        long approvedVendors,
        long rejectedVendors,
        long suspendedVendors
) {
}

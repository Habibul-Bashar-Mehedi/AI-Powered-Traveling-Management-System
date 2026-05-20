package aptms.repositories.projections;

import java.math.BigDecimal;

public interface OrderAnalyticsProjection {
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
    Long getPendingOrders();
    Long getCompletedOrders();
}


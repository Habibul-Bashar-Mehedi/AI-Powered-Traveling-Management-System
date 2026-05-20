package aptms.repositories;

import aptms.entities.AdminOrder;
import aptms.repositories.projections.OrderAnalyticsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface AdminOrderRepository extends JpaRepository<AdminOrder, UUID>, JpaSpecificationExecutor<AdminOrder> {

    @Query("""
            SELECT
              COUNT(o) AS totalOrders,
              COALESCE(SUM(o.totalPrice), 0) AS totalRevenue,
              COALESCE(SUM(CASE WHEN o.status = aptms.enums.AdminOrderStatus.PENDING THEN 1 ELSE 0 END), 0) AS pendingOrders,
              COALESCE(SUM(CASE WHEN o.status = aptms.enums.AdminOrderStatus.COMPLETED THEN 1 ELSE 0 END), 0) AS completedOrders
            FROM AdminOrder o
            """)
    OrderAnalyticsProjection fetchOrderAnalytics();
}


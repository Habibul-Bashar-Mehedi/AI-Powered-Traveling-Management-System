package aptms.services;

import aptms.dto.admin.*;
import aptms.enums.AdminOrderStatus;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AdminDashboardService {

    Page<UserManagementResponse> getUsers(String search, UserRole role, Pageable pageable);

    UserManagementResponse createUser(UserManagementRequest request);

    UserManagementResponse updateUser(UUID userId, UserManagementRequest request);

    void deleteUser(UUID userId);

    Page<ProductResponse> getProducts(String search, Boolean active, Pageable pageable);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(UUID productId, ProductRequest request);

    void deleteProduct(UUID productId);

    Page<OrderResponse> getOrders(AdminOrderStatus status, UUID userId, Instant from, Instant to, Pageable pageable);

    OrderResponse createOrder(OrderRequest request);

    OrderResponse updateOrder(UUID orderId, OrderRequest request);

    void deleteOrder(UUID orderId);

    Page<SystemSettingResponse> getSettings(String search, String category, Boolean active, Pageable pageable);

    SystemSettingResponse createSetting(SystemSettingRequest request, UUID actorUserId);

    SystemSettingResponse updateSetting(UUID settingId, SystemSettingRequest request, UUID actorUserId);

    void deleteSetting(UUID settingId);

    Page<VendorSummaryResponse> getVendors(VendorStatus status, String search, Pageable pageable);

    VendorSummaryResponse approveVendor(UUID vendorId, UUID actorUserId);

    VendorSummaryResponse rejectVendor(UUID vendorId, UUID actorUserId, String reason);

    VendorSummaryResponse suspendVendor(UUID vendorId, UUID actorUserId, String reason);

    AdminDashboardAnalyticsResponse getAnalytics();
}


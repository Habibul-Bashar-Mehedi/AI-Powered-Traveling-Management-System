package aptms.services.impl;

import aptms.dto.admin.*;
import aptms.entities.AdminOrder;
import aptms.entities.Product;
import aptms.entities.SystemSetting;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.AdminOrderStatus;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.AdminOrderRepository;
import aptms.repositories.ProductRepository;
import aptms.repositories.SystemSettingRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.projections.OrderAnalyticsProjection;
import aptms.repositories.projections.VendorAnalyticsProjection;
import aptms.repositories.spec.AdminSpecifications;
import aptms.services.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AdminOrderRepository adminOrderRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementResponse> getUsers(String search, UserRole role, Pageable pageable) {
        return userRepository.findAll(AdminSpecifications.users(search, role), pageable)
                .map(this::toUserResponse);
    }

    @Override
    @Transactional
    public UserManagementResponse createUser(UserManagementRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateValueFoundExceptions("Email already in use: " + request.email());
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setCountryId(request.countryId());
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserManagementResponse updateUser(UUID userId, UserManagementRequest request) {
        User user = getUser(userId);
        if (userRepository.existsByEmailAndIdNot(request.email().trim().toLowerCase(), userId)) {
            throw new DuplicateValueFoundExceptions("Email already in use: " + request.email());
        }

        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setRole(request.role());
        user.setCountryId(request.countryId());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = getUser(userId);
        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String search, Boolean active, Pageable pageable) {
        return productRepository.findAll(AdminSpecifications.products(search, active), pageable)
                .map(this::toProductResponse);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.sku().trim())) {
            throw new DuplicateValueFoundExceptions("SKU already exists: " + request.sku());
        }

        Product product = new Product();
        applyProductPayload(product, request);
        return toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request) {
        Product product = getProduct(productId);
        if (productRepository.existsBySkuAndProductIdNot(request.sku().trim(), productId)) {
            throw new DuplicateValueFoundExceptions("SKU already exists: " + request.sku());
        }

        applyProductPayload(product, request);
        return toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = getProduct(productId);
        productRepository.delete(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(AdminOrderStatus status, UUID userId, Instant from, Instant to, Pageable pageable) {
        return adminOrderRepository.findAll(AdminSpecifications.orders(status, userId, from, to), pageable)
                .map(this::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = getUser(request.userId());
        Product product = getProduct(request.productId());
        validateStock(product, request.quantity());

        AdminOrder order = new AdminOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setProduct(product);
        order.setQuantity(request.quantity());
        order.setUnitPrice(product.getPrice());
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(request.status());

        product.setStockQuantity(product.getStockQuantity() - request.quantity());
        productRepository.save(product);

        return toOrderResponse(adminOrderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(UUID orderId, OrderRequest request) {
        AdminOrder order = getOrder(orderId);
        User user = getUser(request.userId());
        Product newProduct = getProduct(request.productId());

        if (!order.getProduct().getProductId().equals(newProduct.getProductId()) ||
                !order.getQuantity().equals(request.quantity())) {
            // Restore old stock first, then reserve stock for the new payload.
            Product oldProduct = order.getProduct();
            oldProduct.setStockQuantity(oldProduct.getStockQuantity() + order.getQuantity());
            productRepository.save(oldProduct);

            validateStock(newProduct, request.quantity());
            newProduct.setStockQuantity(newProduct.getStockQuantity() - request.quantity());
            productRepository.save(newProduct);
        }

        order.setUser(user);
        order.setProduct(newProduct);
        order.setQuantity(request.quantity());
        order.setUnitPrice(newProduct.getPrice());
        order.setTotalPrice(newProduct.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(request.status());

        return toOrderResponse(adminOrderRepository.save(order));
    }

    @Override
    @Transactional
    public void deleteOrder(UUID orderId) {
        AdminOrder order = getOrder(orderId);
        Product product = order.getProduct();
        product.setStockQuantity(product.getStockQuantity() + order.getQuantity());
        productRepository.save(product);
        adminOrderRepository.delete(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SystemSettingResponse> getSettings(String search, String category, Boolean active, Pageable pageable) {
        return systemSettingRepository.findAll(AdminSpecifications.settings(search, category, active), pageable)
                .map(this::toSettingResponse);
    }

    @Override
    @Transactional
    public SystemSettingResponse createSetting(SystemSettingRequest request, UUID actorUserId) {
        if (systemSettingRepository.existsBySettingKey(request.settingKey().trim())) {
            throw new DuplicateValueFoundExceptions("Setting key already exists: " + request.settingKey());
        }

        SystemSetting setting = new SystemSetting();
        applySettingPayload(setting, request, actorUserId);
        return toSettingResponse(systemSettingRepository.save(setting));
    }

    @Override
    @Transactional
    public SystemSettingResponse updateSetting(UUID settingId, SystemSettingRequest request, UUID actorUserId) {
        SystemSetting setting = getSetting(settingId);
        if (systemSettingRepository.existsBySettingKeyAndSettingIdNot(request.settingKey().trim(), settingId)) {
            throw new DuplicateValueFoundExceptions("Setting key already exists: " + request.settingKey());
        }

        applySettingPayload(setting, request, actorUserId);
        return toSettingResponse(systemSettingRepository.save(setting));
    }

    @Override
    @Transactional
    public void deleteSetting(UUID settingId) {
        SystemSetting setting = getSetting(settingId);
        systemSettingRepository.delete(setting);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VendorSummaryResponse> getVendors(VendorStatus status, String search, Pageable pageable) {
        List<VendorStatus> statuses = status == null
                ? List.of(VendorStatus.PENDING, VendorStatus.PENDING_REVIEW, VendorStatus.APPROVED, VendorStatus.REJECTED, VendorStatus.SUSPENDED)
                : List.of(status);
        String normalizedSearch = normalize(search);
        return vendorRepository.searchByStatuses(statuses, normalizedSearch, pageable)
                .map(this::toVendorSummaryResponse);
    }

    @Override
    @Transactional
    public VendorSummaryResponse approveVendor(UUID vendorId, UUID actorUserId) {
        Vendor vendor = getVendor(vendorId);
        User actor = getUser(actorUserId);

        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setApprovedAt(Instant.now());
        vendor.setApprovedBy(actor);
        vendor.setRejectionReason(null);
        return toVendorSummaryResponse(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public VendorSummaryResponse rejectVendor(UUID vendorId, UUID actorUserId, String reason) {
        Vendor vendor = getVendor(vendorId);
        getUser(actorUserId);

        vendor.setStatus(VendorStatus.REJECTED);
        vendor.setRejectionReason(reason.trim());
        return toVendorSummaryResponse(vendorRepository.save(vendor));
    }

    @Override
    @Transactional
    public VendorSummaryResponse suspendVendor(UUID vendorId, UUID actorUserId, String reason) {
        Vendor vendor = getVendor(vendorId);
        getUser(actorUserId);

        vendor.setStatus(VendorStatus.SUSPENDED);
        vendor.setRejectionReason(reason == null ? null : reason.trim());
        return toVendorSummaryResponse(vendorRepository.save(vendor));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardAnalyticsResponse getAnalytics() {
        OrderAnalyticsProjection orderAnalytics = adminOrderRepository.fetchOrderAnalytics();
        VendorAnalyticsProjection vendorAnalytics = vendorRepository.fetchVendorAnalytics(List.of(VendorStatus.PENDING, VendorStatus.PENDING_REVIEW));

        return new AdminDashboardAnalyticsResponse(
                userRepository.count(),
                productRepository.count(),
                nvl(orderAnalytics.getTotalOrders()),
                orderAnalytics.getTotalRevenue() == null ? BigDecimal.ZERO : orderAnalytics.getTotalRevenue(),
                nvl(orderAnalytics.getPendingOrders()),
                nvl(orderAnalytics.getCompletedOrders()),
                nvl(vendorAnalytics.getTotalVendors()),
                nvl(vendorAnalytics.getPendingVendors()),
                nvl(vendorAnalytics.getApprovedVendors()),
                nvl(vendorAnalytics.getRejectedVendors()),
                nvl(vendorAnalytics.getSuspendedVendors())
        );
    }

    private void applyProductPayload(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setSku(request.sku().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setActive(request.active());
    }

    private void applySettingPayload(SystemSetting setting, SystemSettingRequest request, UUID actorUserId) {
        setting.setSettingKey(request.settingKey().trim());
        setting.setSettingValue(request.settingValue().trim());
        setting.setDescription(request.description());
        setting.setCategory(request.category());
        setting.setActive(request.active());
        setting.setUpdatedByUserId(actorUserId);
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalStateException("Cannot place order for inactive product: " + product.getProductId());
        }
        if (product.getStockQuantity() < requestedQuantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getProductId());
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IdNotFoundException("User not found: " + userId));
    }

    private Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IdNotFoundException("Product not found: " + productId));
    }

    private AdminOrder getOrder(UUID orderId) {
        return adminOrderRepository.findById(orderId)
                .orElseThrow(() -> new IdNotFoundException("Order not found: " + orderId));
    }

    private SystemSetting getSetting(UUID settingId) {
        return systemSettingRepository.findById(settingId)
                .orElseThrow(() -> new IdNotFoundException("Setting not found: " + settingId));
    }

    private Vendor getVendor(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new IdNotFoundException("Vendor not found: " + vendorId));
    }

    private UserManagementResponse toUserResponse(User user) {
        return new UserManagementResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCountryId(),
                user.getFailedLoginAttempts(),
                user.getLockoutUntil(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private OrderResponse toOrderResponse(AdminOrder order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                order.getProduct().getProductId(),
                order.getProduct().getName(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getPlacedAt(),
                order.getUpdatedAt()
        );
    }

    private SystemSettingResponse toSettingResponse(SystemSetting setting) {
        return new SystemSettingResponse(
                setting.getSettingId(),
                setting.getSettingKey(),
                setting.getSettingValue(),
                setting.getDescription(),
                setting.getCategory(),
                setting.getActive(),
                setting.getUpdatedByUserId(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }

    private VendorSummaryResponse toVendorSummaryResponse(Vendor vendor) {
        return new VendorSummaryResponse(
                vendor.getVendorId(),
                vendor.getUser().getId(),
                vendor.getBusinessName(),
                vendor.getEmail(),
                vendor.getPhone(),
                vendor.getVendorType(),
                vendor.getStatus(),
                vendor.getRejectionReason(),
                vendor.getCreatedAt(),
                vendor.getApprovedAt(),
                vendor.getApprovedBy() == null ? null : vendor.getApprovedBy().getId()
        );
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : "%" + value.trim().toLowerCase() + "%";
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }
}


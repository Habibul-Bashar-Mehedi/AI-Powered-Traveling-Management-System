package aptms.api;

import aptms.dto.admin.*;
import aptms.enums.AdminOrderStatus;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import aptms.services.AdminDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/management")
@CrossOrigin(
    origins = {"http://localhost:4200", "http://localhost:3000", "http://127.0.0.1:4200"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS},
    allowedHeaders = "*",
    allowCredentials = "true"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminManagementController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/users")
    public ResponseEntity<Page<UserManagementResponse>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminDashboardService.getUsers(search, role, pageable));
    }

    @PostMapping("/users")
    public ResponseEntity<UserManagementResponse> createUser(@Valid @RequestBody UserManagementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDashboardService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserManagementResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserManagementRequest request
    ) {
        return ResponseEntity.ok(adminDashboardService.updateUser(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        adminDashboardService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminDashboardService.getProducts(search, active, pageable));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDashboardService.createProduct(request));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(adminDashboardService.updateProduct(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        adminDashboardService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestParam(required = false) AdminOrderStatus status,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminDashboardService.getOrders(status, userId, from, to, pageable));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDashboardService.createOrder(request));
    }

    @PutMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest request
    ) {
        return ResponseEntity.ok(adminDashboardService.updateOrder(orderId, request));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId) {
        adminDashboardService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/system-settings")
    public ResponseEntity<Page<SystemSettingResponse>> getSettings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminDashboardService.getSettings(search, category, active, pageable));
    }

    @PostMapping("/system-settings")
    public ResponseEntity<SystemSettingResponse> createSetting(@Valid @RequestBody SystemSettingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminDashboardService.createSetting(request, currentUserId()));
    }

    @PutMapping("/system-settings/{settingId}")
    public ResponseEntity<SystemSettingResponse> updateSetting(
            @PathVariable UUID settingId,
            @Valid @RequestBody SystemSettingRequest request
    ) {
        return ResponseEntity.ok(adminDashboardService.updateSetting(settingId, request, currentUserId()));
    }

    @DeleteMapping("/system-settings/{settingId}")
    public ResponseEntity<Void> deleteSetting(@PathVariable UUID settingId) {
        adminDashboardService.deleteSetting(settingId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vendors")
    public ResponseEntity<Page<VendorSummaryResponse>> getVendors(
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(adminDashboardService.getVendors(status, search, pageable));
    }

    @PatchMapping("/vendors/{vendorId}/approve")
    public ResponseEntity<VendorSummaryResponse> approveVendor(@PathVariable UUID vendorId) {
        return ResponseEntity.ok(adminDashboardService.approveVendor(vendorId, currentUserId()));
    }

    @PatchMapping("/vendors/{vendorId}/reject")
    public ResponseEntity<VendorSummaryResponse> rejectVendor(
            @PathVariable UUID vendorId,
            @Valid @RequestBody VendorDecisionRequest request
    ) {
        return ResponseEntity.ok(adminDashboardService.rejectVendor(vendorId, currentUserId(), request.reason()));
    }

    @PatchMapping("/vendors/{vendorId}/suspend")
    public ResponseEntity<VendorSummaryResponse> suspendVendor(
            @PathVariable UUID vendorId,
            @Valid @RequestBody(required = false) VendorSuspendRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(adminDashboardService.suspendVendor(vendorId, currentUserId(), reason));
    }

    @GetMapping("/analytics")
    public ResponseEntity<AdminDashboardAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminDashboardService.getAnalytics());
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }
}


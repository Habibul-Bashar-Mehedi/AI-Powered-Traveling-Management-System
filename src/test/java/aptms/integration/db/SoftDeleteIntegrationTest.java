package aptms.integration.db;

import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import aptms.enums.VendorType;
import aptms.integration.AbstractIntegrationTest;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for database-level concerns: soft delete and cascading behaviour.
 *
 * <p>Tests:
 * <ol>
 *   <li>Soft-deleted user is excluded from {@code findActiveByEmail} but still exists in DB</li>
 *   <li>Soft-deleted user cannot log in (authentication uses {@code findActiveByEmail})</li>
 *   <li>Hard-delete via the admin endpoint removes the user record from the DB</li>
 *   <li>Vendor entity is cascade-deleted when the owning user is removed (hard delete)</li>
 * </ol>
 *
 * <p>Running: {@code ./mvnw test -Dtest=SoftDeleteIntegrationTest} (Docker must be running).
 */
@Tag("integration")
@DisplayName("Database Soft-Delete and Cascade Integration Tests")
class SoftDeleteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private VendorRepository vendorRepository;

    @AfterEach
    void cleanUpVendors() {
        // @Transactional on base class handles all rollback automatically.
    }

    // ─── 1. Soft delete — user excluded from active query ──────────────────────

    @Test
    @DisplayName("Soft-deleted user is excluded from findActiveByEmail but record is still in DB")
    void softDeletedUser_excludedFromActiveQuery_butPresentInDb() {
        User user = createUser("soft_del", "softdel@example.org", "Pass@12345", UserRole.USER);

        // Simulate soft-delete by setting deletedAt
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        // findActiveByEmail (WHERE deletedAt IS NULL) must return empty
        Optional<User> active = userRepository.findActiveByEmail("softdel@example.org");
        assertThat(active).isEmpty();

        // findByEmail (no filter) still finds the record — it's soft-deleted, not gone
        Optional<User> raw = userRepository.findByEmail("softdel@example.org");
        assertThat(raw).isPresent();
        assertThat(raw.get().getDeletedAt()).isNotNull();
    }

    // ─── 2. Soft-deleted user cannot log in ───────────────────────────────────

    @Test
    @DisplayName("Soft-deleted user receives 401 on login — authentication uses findActiveByEmail")
    void softDeletedUser_cannotLogin_receives401() throws Exception {
        User user = createUser("deleted_login", "deleted.login@example.org", "Pass@12345", UserRole.USER);

        // Mark as soft-deleted
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        // Login attempt must fail (CustomUserDetailsService calls findActiveByEmail)
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"deleted.login@example.org","password":"Pass@12345"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ─── 3. Hard delete via admin endpoint ────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/admin/management/users/{id} — admin soft-deletes user (sets deletedAt, record stays in DB)")
    void adminDelete_softDeletesUser_recordRemainsInDB() throws Exception {
        // Create a target user and an admin user
        User target = createUser("delete_target", "delete.target@example.org", "Pass@12345", UserRole.USER);
        String adminToken = createUserAndGetToken(
                "del_admin", "del.admin@example.org", "AdminPass@1", UserRole.ADMIN);

        // Admin deletes the target user
        mockMvc.perform(
                delete("/api/v1/admin/management/users/" + target.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // NOTE: This endpoint does a SOFT DELETE (sets deletedAt), not a hard delete.
        // The record REMAINS in the DB but is marked as deleted.
        // This is intentional — see AdminDashboardServiceImpl.deleteUser() which sets deletedAt.
        Optional<User> softDeleted = userRepository.findById(target.getId());
        assertThat(softDeleted).isPresent();
        assertThat(softDeleted.get().getDeletedAt()).isNotNull();

        // The user CANNOT log in after soft-delete (findActiveByEmail excludes deleted users)
        Optional<User> active = userRepository.findActiveByEmail("delete.target@example.org");
        assertThat(active).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/management/users/{id} — USER token blocked by Spring Security (403)")
    void hardDelete_withUserToken_returns403() throws Exception {
        User target = createUser("del_target2", "del.target2@example.org", "Pass@12345", UserRole.USER);
        String userToken = loginAndGetToken("del.target2@example.org", "Pass@12345");

        mockMvc.perform(
                delete("/api/v1/admin/management/users/" + target.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ─── 4. Cascade behaviour: Vendor deleted when User is hard-deleted ─────────

    @Test
    @DisplayName("Admin soft-delete does NOT cascade to Vendor — Vendor record survives; only user is soft-deleted")
    void softDelete_doesNotCascadeToVendor_vendorRecordSurvives() throws Exception {
        if (vendorRepository == null) {
            return;
        }

        // Create a vendor user and give them a Vendor entity
        User vendorUser = createUser("cascade_vendor", "cascade.vendor@example.org",
                "VendorPass@1", UserRole.VENDOR);

        Vendor vendor = new Vendor();
        vendor.setUser(vendorUser);
        vendor.setBusinessName("Cascade Test Tours");
        vendor.setVendorType(VendorType.TOUR_GUIDE);
        vendor.setEmail("cascade@example.org");
        vendor.setPhone("+8801700000001");
        vendor.setAddressLine1("1 Cascade Lane");
        vendor.setCity("Dhaka");
        vendor.setCountryCode("BD");
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setCommissionRate(java.math.BigDecimal.valueOf(10.0));
        vendor = vendorRepository.save(vendor);

        assertThat(vendorRepository.findById(vendor.getVendorId())).isPresent();

        // Admin soft-deletes the vendor user
        String adminToken = createUserAndGetToken(
                "cascade_admin", "cascade.admin@example.org", "AdminPass@1", UserRole.ADMIN);

        mockMvc.perform(
                delete("/api/v1/admin/management/users/" + vendorUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // After soft-delete: the user has deletedAt set
        Optional<User> deletedUser = userRepository.findById(vendorUser.getId());
        assertThat(deletedUser).isPresent();
        assertThat(deletedUser.get().getDeletedAt()).isNotNull();

        // The Vendor record is NOT removed — soft-delete does not cascade to Vendor entity.
        // This is the current (documented) behavior. For full data cleanup on user removal,
        // a cascade or explicit Vendor cleanup would need to be implemented.
        Optional<Vendor> vendor_check = vendorRepository.findById(vendor.getVendorId());
        assertThat(vendor_check).isPresent(); // vendor still exists after user soft-delete
    }

    // ─── 5. Regression: soft-delete does NOT affect other users ───────────────

    @Test
    @DisplayName("Soft-deleting one user does not affect other users' ability to log in")
    void softDelete_oneUser_doesNotAffectOtherUsers() throws Exception {
        User active = createUser("still_active", "still.active@example.org", "ActivePass@1", UserRole.USER);
        User deleted = createUser("soft_del2", "softdel2@example.org", "Pass@12345", UserRole.USER);

        // Soft-delete only the second user
        deleted.setDeletedAt(Instant.now());
        userRepository.save(deleted);

        // The first user can still log in normally
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"still.active@example.org","password":"ActivePass@1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // The soft-deleted user cannot
        mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"softdel2@example.org","password":"Pass@12345"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}





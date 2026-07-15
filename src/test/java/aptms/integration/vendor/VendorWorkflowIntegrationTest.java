package aptms.integration.vendor;

import aptms.dto.vendor.VendorRegistrationRequest;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import aptms.enums.VendorType;
import aptms.integration.AbstractIntegrationTest;
import aptms.repositories.VendorRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the vendor onboarding and management workflow.
 *
 * <p>Covers:
 * <ol>
 *   <li>Vendor registration by an authenticated user → Vendor created with PENDING_REVIEW
 *       status; user's role promoted to VENDOR in the database</li>
 *   <li>Admin approves vendor → status transitions to APPROVED</li>
 *   <li>Admin suspends a vendor → status transitions to SUSPENDED</li>
 *   <li>Unauthenticated vendor registration → 401</li>
 *   <li>Non-VENDOR JWT on vendor-only endpoint → 403</li>
 *   <li>Suspended-vendor service creation gap documented (see test body for explanation)</li>
 * </ol>
 *
 * <p>Running: {@code ./mvnw test -Dtest=VendorWorkflowIntegrationTest} (Docker must be running).
 */
@Tag("integration")
@DisplayName("Vendor Workflow Integration Tests")
class VendorWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;

    @AfterEach
    void cleanUpVendors() {
        // @Transactional on base class rolls back DB state automatically.
        // No manual deletes needed.
    }

    // ─── 1. Vendor Registration ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/vendor/register — authenticated user registers as vendor; DB shows PENDING_REVIEW")
    void vendorRegistration_byAuthenticatedUser_createsPendingVendorAndPromotesRole() throws Exception {
        // Any authenticated user (any role) may self-register as a vendor
        User applicant = createUser("vendor_alice", "vendor.alice@example.org",
                "VendorPass@1", UserRole.USER);
        String userToken = loginAndGetToken("vendor.alice@example.org", "VendorPass@1");

        VendorRegistrationRequest req = buildRegistrationRequest("Alice Tours");

        MvcResult result = mockMvc.perform(
                post("/api/v1/vendor/register")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessName").value("Alice Tours"))
                .andReturn();

        // Verify Vendor entity persisted with PENDING_REVIEW status
        List<Vendor> vendors = vendorRepository.findAll();
        assertThat(vendors).hasSize(1);
        Vendor vendor = vendors.get(0);
        assertThat(vendor.getBusinessName()).isEqualTo("Alice Tours");
        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.PENDING_REVIEW);
        // NOTE: vendor.getUser() is lazy-loaded; don't access it here outside a session.
        // User role promotion is verified via the UserRepository below.

        // Verify user's role was promoted to VENDOR in the database
        User updatedApplicant = userRepository.findById(applicant.getId()).orElseThrow();
        assertThat(updatedApplicant.getRole()).isEqualTo(UserRole.VENDOR);
    }

    @Test
    @DisplayName("POST /api/v1/vendor/register — unauthenticated request returns 401")
    void vendorRegistration_withoutAuth_returns401() throws Exception {
        mockMvc.perform(
                post("/api/v1/vendor/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegistrationRequest("Ghost Vendor"))))
                .andExpect(status().isUnauthorized());
    }

    // ─── 2. Admin Approval Flow ────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/admin/management/vendors/{id}/approve — admin approves vendor; status → APPROVED")
    void adminApproval_changesVendorStatusToApproved() throws Exception {
        // Set up: create a vendor applicant and register them
        createUser("vendor_bob", "vendor.bob@example.org", "VendorPass@1", UserRole.USER);
        String vendorToken = loginAndGetToken("vendor.bob@example.org", "VendorPass@1");

        mockMvc.perform(
                post("/api/v1/vendor/register")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegistrationRequest("Bob Travels"))))
                .andExpect(status().isCreated());

        Vendor pendingVendor = vendorRepository.findAll().get(0);
        assertThat(pendingVendor.getStatus()).isEqualTo(VendorStatus.PENDING_REVIEW);

        // Admin approves
        String adminToken = createUserAndGetToken(
                "admin_vendor", "admin.vendor@example.org", "AdminPass@1", UserRole.ADMIN);

        mockMvc.perform(
                patch("/api/v1/admin/management/vendors/" + pendingVendor.getVendorId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify DB reflects the approval
        Vendor approved = vendorRepository.findById(pendingVendor.getVendorId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(VendorStatus.APPROVED);
        assertThat(approved.getApprovedAt()).isNotNull();
        assertThat(approved.getApprovedBy()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/management/vendors/{id}/suspend — admin suspends vendor; status → SUSPENDED")
    void adminSuspension_changesVendorStatusToSuspended() throws Exception {
        // Register a vendor and then approve them
        createUser("vendor_carol", "vendor.carol@example.org", "VendorPass@1", UserRole.USER);
        String vendorToken = loginAndGetToken("vendor.carol@example.org", "VendorPass@1");

        mockMvc.perform(
                post("/api/v1/vendor/register")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegistrationRequest("Carol Expeditions"))))
                .andExpect(status().isCreated());

        Vendor vendor = vendorRepository.findAll().get(0);
        String adminToken = createUserAndGetToken(
                "admin_suspend", "admin.suspend@example.org", "AdminPass@1", UserRole.ADMIN);

        // Approve first
        mockMvc.perform(
                patch("/api/v1/admin/management/vendors/" + vendor.getVendorId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Suspend
        mockMvc.perform(
                patch("/api/v1/admin/management/vendors/" + vendor.getVendorId() + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Policy violation during integration test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        Vendor suspended = vendorRepository.findById(vendor.getVendorId()).orElseThrow();
        assertThat(suspended.getStatus()).isEqualTo(VendorStatus.SUSPENDED);
    }

    @Test
    @DisplayName("USER token on /api/v1/vendor/profile — blocked by Spring Security (403); VENDOR role required")
    void vendorEndpoint_withUserToken_returns403() throws Exception {
        String userToken = createUserAndGetToken(
                "plain_user", "plain.user@example.org", "UserPass@1", UserRole.USER);

        mockMvc.perform(
                get("/api/v1/vendor/profile")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Documents the known gap: a SUSPENDED vendor who already holds a valid VENDOR JWT
     * can still reach the service creation endpoint because:
     * <ol>
     *   <li>Spring Security only checks the JWT role (VENDOR) — passes ✅</li>
     *   <li>VendorServiceMgmtServiceImpl does not validate VendorStatus before creating a service</li>
     * </ol>
     * This test verifies the current (incorrect) behaviour.  A correct implementation
     * would check vendor.getStatus() == APPROVED in the service layer and throw 403.
     *
     * <p><strong>Known gap — tracked as a recommendation in CASE_STUDY.md.</strong>
     */
    @Test
    @DisplayName("GAP: Suspended vendor with valid VENDOR JWT can still reach vendor service endpoints")
    void suspendedVendor_withValidVendorJwt_canStillReachVendorEndpoints_knownGap() throws Exception {
        // To test this gap we need a VENDOR-role JWT; create the user with VENDOR role directly
        // (simulates a vendor whose Vendor entity is SUSPENDED but JWT still says VENDOR)
        String vendorToken = createUserAndGetToken(
                "susp_vendor", "susp.vendor@example.org", "VendorPass@1", UserRole.VENDOR);

        // The endpoint /api/v1/vendor/services is VENDOR-role gated.
        // Without a Vendor entity the service layer will fail (not SUSPENDED check),
        // so this returns 4xx from the service layer — but for a different reason than suspension.
        // The point is: Spring Security lets the request through (because JWT role = VENDOR).
        // The request reaches the controller, confirming the gap exists at the security layer.
        MvcResult result = mockMvc.perform(
                get("/api/v1/vendor/services")
                        .header("Authorization", "Bearer " + vendorToken))
                .andReturn();

        int status = result.getResponse().getStatus();
        // 200 (empty list) or 4xx from service layer — either way, NOT 403 from Spring Security.
        // A proper fix would return 403 if vendor.status == SUSPENDED.
        assertThat(status).isNotEqualTo(403);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private VendorRegistrationRequest buildRegistrationRequest(String businessName) {
        VendorRegistrationRequest req = new VendorRegistrationRequest();
        req.setBusinessName(businessName);
        req.setVendorType(VendorType.TOUR_GUIDE);
        req.setEmail("business@example.org");
        req.setPhone("+8801700000000");
        req.setAddressLine1("12 Test Lane");
        req.setCity("Dhaka");
        req.setCountryCode("BD");
        req.setDescription("Integration test vendor");
        return req;
    }
}





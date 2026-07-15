package aptms.integration;

import aptms.dto.AuthResponse;
import aptms.dto.LoginRequest;
import aptms.entities.OtpVerification;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.repositories.OtpVerificationRepository;
import aptms.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for all SMTS integration tests.
 *
 * <p>Starts one MySQL 8.4 container and one Redis 7 container per JVM run (static fields
 * mean containers are reused across all subclasses via Testcontainers' singleton container
 * pattern).  Spring Boot's {@code @ServiceConnection} automatically wires the MySQL
 * datasource; Redis is wired via {@link DynamicPropertySource}.
 *
 * <p>The {@code integration-test} Spring profile activates
 * {@code application-integration-test.properties}, which disables Flyway and sets
 * {@code ddl-auto=create-drop} so Hibernate builds a fresh schema in the containerised
 * MySQL database.
 *
 * <h2>Cleanup strategy</h2>
 * Each test method runs in a real committed transaction (no test-level {@code @Transactional})
 * so that MockMvc requests exercise the exact same code path as production.  After each test,
 * {@link #cleanUpDatabase()} disables FK checks, truncates all tables, then re-enables FK
 * checks — guaranteeing a clean slate regardless of entity creation order.
 *
 * <p>Note: We intentionally do NOT use {@code @Transactional} on the test class because
 * {@code BookingService.isRoomBooked()} uses {@code @Lock(PESSIMISTIC_WRITE)}, which
 * conflicts with the outer test transaction when running via MockMvc in the same thread.
 *
 * <h2>Running the integration tests</h2>
 * <pre>
 *   # Docker must be running (DOCKER_HOST env var or /var/run/docker.sock).
 *   DOCKER_HOST=unix:///var/run/docker.sock ./mvnw test -Dgroups=integration -DforkCount=0
 *   # or to run everything including unit tests:
 *   DOCKER_HOST=unix:///var/run/docker.sock ./mvnw verify -DforkCount=0
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    // ─── Containers ───────────────────────────────────────────────────────────

    @Container
    @ServiceConnection
    static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("travel_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    // ─── Spring beans ─────────────────────────────────────────────────────────

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JsonMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected OtpVerificationRepository otpVerificationRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    protected RedisTemplate<String, String> redisTemplate;

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    /**
     * After each test: truncate all application tables and flush the Redis blacklist.
     * FK checks are disabled for the duration of the truncation to avoid ordering constraints.
     * Tables that don't exist (e.g. because no data was written, or the name differs) are
     * silently skipped.
     */
    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : ALL_TABLES) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
            } catch (Exception ignored) {
                // Table may not exist if no data was written to it; safe to skip
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        flushRedisBlacklist();
    }

    /**
     * All application tables in the test schema, with EXACT names from @Table annotations.
     * FK checks are disabled during truncation so order doesn't matter.
     */
    private static final String[] ALL_TABLES = {
            // Auth / user domain
            "users", "users_aud", "refresh_tokens", "token_blacklist",
            // Booking domain
            "booking", "booking_aud",
            // Hotel domain
            "hotels", "hotels_aud", "rooms", "rooms_aud",
            // Vendor domain
            "vendor", "vendor_document", "vendor_service", "vendor_booking",
            "wallet_transaction", "payout_request", "service_availability",
            // Travel content (use exact @Table names from entity classes)
            "destination", "destination_aud",
            "tourist_spots",       // TouristSpot entity
            "transports",          // Transport entity
            "traditional_foods",   // TraditionalFood entity
            "traditionalItems",    // TraditionalItem entity
            "Markets",             // Market entity
            "routes",              // Route entity
            "package_item",
            // OTP
            "otp_verifications",
            // Admin / misc
            "admin_order", "admin_product", "banner", "system_setting", "chatHistories",
            // Envers revision info
            "revinfo"
    };

    // ─── Shared helpers ───────────────────────────────────────────────────────

    /**
     * Creates a user directly in the database with an encoded password.
     * Bypasses the registration API (and its blocked-domain check).
     * Use {@code @example.org} domain — not on the application's blocklist.
     */
    protected User createUser(String username, String email, String rawPassword, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    /**
     * POSTs to {@code /api/auth/login} and extracts the JWT access token.
     * Asserts that login returns HTTP 200.
     */
    protected String loginAndGetToken(String email, String rawPassword) throws Exception {
        LoginRequest request = new LoginRequest(email, rawPassword);
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getAccessToken();
    }

    /** Creates a user and immediately logs in, returning the JWT. */
    protected String createUserAndGetToken(String username, String email,
                                           String rawPassword, UserRole role) throws Exception {
        createUser(username, email, rawPassword, role);
        return loginAndGetToken(email, rawPassword);
    }

    /**
     * Flushes all Redis keys whose names start with {@code "blacklist:"}.
     */
    protected void flushRedisBlacklist() {
        if (redisTemplate != null) {
            Set<String> keys = redisTemplate.keys("blacklist:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    /**
     * Reads the currently-stored OTP code for an email directly from the database
     * (test-only shortcut — no real mail server is available in integration tests).
     */
    protected String readOtpFromDatabase(String email) {
        return otpVerificationRepository.findByEmail(email)
            .map(OtpVerification::getOtpCode)
            .orElse(null);
    }
}




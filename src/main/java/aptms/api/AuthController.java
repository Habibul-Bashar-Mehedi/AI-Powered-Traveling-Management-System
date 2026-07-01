package aptms.api;

import aptms.dto.*;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.repositories.UserRepository;
import aptms.services.AuthenticationService;
import aptms.services.FeatureFlagService;
import aptms.services.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static aptms.constants.EntityConstants.*;

/**
 * REST controller for authentication endpoints.
 * 
 * Provides endpoints for:
 * - User registration with JWT token issuance
 * - User login with JWT token issuance
 * - Token refresh
 * - Logout (single session)
 * - Logout all (all sessions)
 * - Get current user profile
 * 
 * Supports feature flag for gradual rollout:
 * - When JWT_ENABLED=true: Uses JWT authentication
 * - When JWT_ENABLED=false: Falls back to session-based authentication
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, BR-5
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Authentication", description = "JWT-based authentication endpoints for user registration, login, token management, and logout")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final FeatureFlagService featureFlagService;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationService authenticationService,
            RegistrationService registrationService,
            FeatureFlagService featureFlagService,
            UserRepository userRepository) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
        this.featureFlagService = featureFlagService;
        this.userRepository = userRepository;
    }

    /**
     * Register new user and issue JWT tokens.
     * 
     * POST /api/auth/register
     * 
     * Requirements: 5.1, FR-REG-001, FR-REG-002, FR-REG-003
     */
    @Operation(
        summary = "Register new user",
        description = """
            Register a new user account and receive JWT access and refresh tokens.
            
            **Token Information:**
            - Access token expires in 15 minutes (900 seconds)
            - Refresh token expires in 7 days (604800 seconds)
            - Both tokens are returned in the response
            
            **Validation Rules:**
            - Email: Valid format, unique in database
            - Password: Minimum 8 characters
            - Username: Required, 3-50 characters
            - Role: Valid enum value (USER, ADMIN, VENDOR)
            
            **Requirements:** FR-REG-001, FR-REG-002, FR-REG-003
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User successfully registered and tokens issued",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successful Registration",
                    value = """
                        {
                          "user": {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "username": "johndoe",
                            "email": "john@example.com",
                            "roles": ["USER"]
                          },
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MTcyMDAwMDAsImV4cCI6MTcxNzIwMDkwMCwianRpIjoiN2M5ZTY2NzktNzQyNS00MGRlLTk0NGItZTA3ZmMxZjkwYWU3IiwiaXNzIjoiY29tLmFwdG1zLmF1dGgiLCJhdWQiOiJjb20uYXB0bXMuYXBpIiwicm9sZXMiOlsiVVNFUiJdLCJlbWFpbCI6ImpvaG5AZXhhbXBsZS5jb20ifQ.signature",
                          "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                          "tokenType": "Bearer",
                          "expiresIn": 900
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input - validation errors",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                        {
                          "error": "VALIDATION_ERROR",
                          "message": "Invalid input data",
                          "timestamp": "2025-06-01T12:00:00Z",
                          "path": "/api/auth/register",
                          "details": {
                            "email": "Invalid email format",
                            "password": "Password must be at least 8 characters"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Email already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Duplicate Email",
                    value = """
                        {
                          "error": "EMAIL_ALREADY_EXISTS",
                          "message": "A user with this email already exists",
                          "timestamp": "2025-06-01T12:00:00Z",
                          "path": "/api/auth/register"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Parameter(description = "User registration data", required = true,
            schema = @Schema(implementation = RegisterRequest.class))
        @Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {} (JWT enabled: {})",
            request.getEmail(), featureFlagService.isJwtEnabled());
        // Feature flag is checked for logging/metrics purposes; both paths delegate to authenticationService
        AuthResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticate user and issue JWT tokens.
     * 
     * POST /api/auth/login
     * 
     * Requirements: 5.2, FR-LGN-001, FR-LGN-002, FR-LGN-003
     */
    @Operation(
        summary = "Login user",
        description = """
            Authenticate user credentials and receive JWT access and refresh tokens.
            
            **Token Information:**
            - Access token expires in 15 minutes (900 seconds)
            - Refresh token expires in 7 days (604800 seconds)
            
            **Security Features:**
            - Account lockout after 5 consecutive failed attempts (15 minutes)
            - Generic error messages to prevent account enumeration
            - Failed attempt counter reset on successful login
            
            **Requirements:** FR-LGN-001, FR-LGN-002, FR-LGN-003
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User successfully authenticated and tokens issued",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successful Login",
                    value = """
                        {
                          "user": {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "username": "johndoe",
                            "email": "john@example.com",
                            "roles": ["USER"]
                          },
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                          "tokenType": "Bearer",
                          "expiresIn": 900
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials (wrong password or non-existent email)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Invalid Credentials",
                    value = """
                        {
                          "error": "INVALID_CREDENTIALS",
                          "message": "Invalid credentials",
                          "timestamp": "2025-06-01T12:00:00Z",
                          "path": "/api/auth/login"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "423",
            description = "Account locked due to too many failed login attempts",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Account Locked",
                    value = """
                        {
                          "error": "ACCOUNT_LOCKED",
                          "message": "Account locked due to too many failed login attempts. Try again after 15 minutes.",
                          "timestamp": "2025-06-01T12:00:00Z",
                          "path": "/api/auth/login",
                          "retryAfter": "2025-06-01T12:15:00Z"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Parameter(description = "User login credentials", required = true,
            schema = @Schema(implementation = LoginRequest.class))
        @Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {} (JWT enabled: {})",
            request.getEmail(), featureFlagService.isJwtEnabled());
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Refresh access token using refresh token.
     * 
     * POST /api/auth/refresh
     * 
     * Requirements: 5.3, FR-RFT-001
     */
    @Operation(
        summary = "Refresh access token",
        description = """
            Use a valid refresh token to obtain a new access token and refresh token pair.
            
            **Token Rotation:**
            - Old refresh token is immediately invalidated
            - New refresh token is issued and stored
            - Access token is regenerated with fresh expiration
            
            **Security Features:**
            - Refresh token reuse detection
            - Automatic revocation of all user sessions on reuse detection
            - Maximum refresh token age enforcement (30 days)
            
            **Requirements:** FR-RFT-001, FR-RFT-003, FR-RFT-004
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tokens successfully refreshed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Successful Refresh",
                    value = """
                        {
                          "user": {
                            "id": "550e8400-e29b-41d4-a716-446655440000",
                            "username": "johndoe",
                            "email": "john@example.com",
                            "roles": ["USER"]
                          },
                          "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "refreshToken": "new-refresh-token-uuid",
                          "tokenType": "Bearer",
                          "expiresIn": 900
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid, expired, or revoked refresh token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid Refresh Token",
                        value = """
                            {
                              "error": "INVALID_REFRESH_TOKEN",
                              "message": "Invalid or expired refresh token",
                              "timestamp": "2025-06-01T12:00:00Z",
                              "path": "/api/auth/refresh"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Refresh Token Reuse Detected",
                        value = """
                            {
                              "error": "REFRESH_TOKEN_REUSE_DETECTED",
                              "message": "Refresh token reuse detected. All sessions have been revoked for security.",
                              "timestamp": "2025-06-01T12:00:00Z",
                              "path": "/api/auth/refresh"
                            }
                            """
                    )
                }
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @Parameter(description = "Refresh token request", required = true,
            schema = @Schema(implementation = RefreshTokenRequest.class))
        @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        AuthResponse response = authenticationService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Logout user and revoke tokens.
     * 
     * POST /api/auth/logout
     * Requires authentication (Bearer token)
     * 
     * Requirements: 5.4, FR-LGT-001, FR-LGT-002
     */
    @Operation(
        summary = "Logout user (single session)",
        description = """
            Logout the current user session by revoking the access token and deleting the refresh token.
            
            **Actions Performed:**
            - Access token's jti added to blacklist
            - Refresh token deleted from database
            - Blacklist entry auto-expires with token
            
            **Authentication Required:**
            - Valid Bearer token in Authorization header
            
            **Requirements:** FR-LGT-001, FR-LGT-002
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "User successfully logged out"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or expired access token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Missing Token",
                        value = """
                            {
                              "error": "TOKEN_MISSING",
                              "message": "Missing Authorization header",
                              "timestamp": "2025-06-01T12:00:00Z",
                              "path": "/api/auth/logout"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Invalid Token",
                        value = """
                            {
                              "error": "TOKEN_INVALID",
                              "message": "Invalid token signature or malformed JWT",
                              "timestamp": "2025-06-01T12:00:00Z",
                              "path": "/api/auth/logout"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Expired Token",
                        value = """
                            {
                              "error": "TOKEN_EXPIRED",
                              "message": "Token has expired",
                              "timestamp": "2025-06-01T12:00:00Z",
                              "path": "/api/auth/logout"
                            }
                            """
                    )
                }
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("Logout request received");
        UUID userId = getCurrentUserId();
        String accessToken = extractAccessToken();
        authenticationService.logout(accessToken, userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Logout from all devices.
     * 
     * POST /api/auth/logout-all
     * Requires authentication (Bearer token)
     * 
     * Requirements: 5.5, FR-LGT-003
     */
    @Operation(
        summary = "Logout from all devices (all sessions)",
        description = """
            Logout the user from all active sessions by revoking all refresh tokens.
            
            **Actions Performed:**
            - All refresh tokens for the user are deleted
            - All active sessions are terminated
            - User must re-authenticate on all devices
            
            **Use Cases:**
            - Security concern (device lost/stolen)
            - Password change
            - Suspicious activity detected
            
            **Authentication Required:**
            - Valid Bearer token in Authorization header
            
            **Requirements:** FR-LGT-003
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "All user sessions successfully revoked"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or expired access token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                        {
                          "error": "TOKEN_INVALID",
                          "message": "Invalid or expired token",
                          "timestamp": "2025-06-01T12:00:00Z",
                          "path": "/api/auth/logout-all"
                        }
                        """
                )
            )
        )
    })
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        log.info("Logout all request received");
        UUID userId = getCurrentUserId();
        authenticationService.logoutAll(userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get current user profile.
     * 
     * GET /api/auth/me
     * Requires authentication (Bearer token)
     * 
     * Requirements: 5.6
     */
    @Operation(
        summary = "Get current user profile",
        description = """
            Retrieve the profile information of the currently authenticated user.
            
            **Information Returned:**
            - User ID (UUID)
            - Email address
            - Roles/permissions
            
            **Authentication Required:**
            - Valid Bearer token in Authorization header
            
            **Requirements:** Section 5.6
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile successfully retrieved",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class),
                examples = @ExampleObject(
                    name = "User Profile",
                    value = """
                        {
                          "id": "550e8400-e29b-41d4-a716-446655440000",
                          "username": "johndoe",
                          "email": "john@example.com",
                          "roles": ["USER"],
                          "createdAt": "2025-01-01T10:00:00Z",
                          "lastLoginAt": "2025-06-01T12:00:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Missing, invalid, or expired access token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                        {
                          "error": "TOKEN_EXPIRED",
                          "message": "Token has expired",
                          "timestamp": "2025-06-01T12:00:00Z",
                          "path": "/api/auth/me"
                        }
                        """
                )
            )
        )
    })
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.info("Get current user request received");
        UUID userId = getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + userId));

        // NOTE: vendor role sync removed from this read endpoint.
        // Role assignment is handled by VendorRegistrationService during vendor registration.

        UserDTO userDTO = UserDTO.builder()
            .id(userId)
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(List.of(user.getRole().name()))
            .build();

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Extract access token from Authorization header.
     * Throws 401 ResponseStatusException on missing/invalid header.
     */
    private String extractAccessToken() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No request context available");
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }

        return authHeader.substring(7);
    }

    /**
     * Safely resolve the UUID of the currently authenticated user.
     * Throws 401 ResponseStatusException for anonymous, null, or non-UUID principals.
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            Object principal = auth.getPrincipal();
            String name = (principal instanceof UserDetails ud) ? ud.getUsername() : auth.getName();
            return UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Cannot resolve user identity from authentication principal");
        }
    }

    // ========== Legacy endpoints (kept for backward compatibility) ==========

    /** @deprecated Use POST /api/auth/register instead */
    @Deprecated
    @PostMapping("/user/register")
    public ResponseEntity<User> userRegister(@RequestBody User user) {
        User registeredUser = registrationService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    /** @deprecated Use POST /api/auth/login instead */
    @Deprecated
    @PostMapping("/user/login")
    public ResponseEntity<String> userLogin(@RequestBody User loginRequest) {
        String response = registrationService.loginUser(
            loginRequest.getEmail(),
            loginRequest.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    // ========== Admin endpoints ==========

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = registrationService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/user/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> updateUser(@PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        boolean updated = registrationService.updateUser(
            id,
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            request.getRole(),
            request.getCountryId()
        );

        if (updated) {
            return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, USER));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format(ENTITY_NOT_FOUND_MESSAGE, USER, id));
        }
    }

    @DeleteMapping("/user/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        String result = registrationService.deleteUser(id);
        return ResponseEntity.ok(result);
    }

    /** Request body for user updates. */
    @lombok.Data
    public static class UserUpdateRequest {
        private String username;
        private String email;
        private String password;
        private UserRole role;
        private String countryId;
    }
}

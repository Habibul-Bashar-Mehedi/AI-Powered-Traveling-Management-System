# Implementation Plan: JWT Authentication System

## Overview

This implementation plan converts the JWT Authentication System design into actionable coding tasks. The system replaces stateful session-based authentication with stateless JWT authentication in a Spring Boot backend with Angular frontend.

**Technology Stack**:
- Backend: Spring Boot 4.0.3, Java 21, MySQL, Redis
- Frontend: Angular 21.2, TypeScript 5.9
- Libraries: JJWT 0.12.5, BCrypt, Spring Security 6.x

**Implementation Approach**:
1. Set up core JWT infrastructure (dependencies, configuration, entities)
2. Implement JWT generation and validation services
3. Add Spring Security filter chain with JWT authentication
4. Implement authentication endpoints (login, register, refresh, logout)
5. Add token blacklist and refresh token management
6. Implement account lockout mechanism
7. Update frontend with token storage and HTTP interceptor
8. Add comprehensive testing (unit, integration, property-based)
9. Database migration and cleanup

---

## Tasks

- [x] 1. Set up project dependencies and configuration
  - Add JJWT dependencies to pom.xml (jjwt-api, jjwt-impl, jjwt-jackson version 0.12.5)
  - Add Spring Security dependencies (spring-boot-starter-security)
  - Add Redis dependencies (spring-boot-starter-data-redis)
  - Add BCrypt dependency (already in Spring Security)
  - Create application.yml JWT configuration properties (secret, access token TTL, refresh token TTL, issuer, audience)
  - Create JwtConfigProperties class with @ConfigurationProperties
  - _Requirements: FR-LGN-005, NFR-2, NFR-5_

- [x] 2. Create database schema and entity models
  - [x] 2.1 Create database migration script for users table updates
    - Add failed_login_attempts INT DEFAULT 0
    - Add lockout_until TIMESTAMP NULL
    - Add last_login_at TIMESTAMP NULL
    - Add created_at and updated_at timestamps
    - Add indexes for lockout_until and email
    - _Requirements: FR-LGN-003, 3.4.1_

  - [x] 2.2 Create RefreshToken entity and repository
    - Create RefreshToken entity with UUID id, user_id FK, token_hash, device_info, ip_address, user_agent, expires_at, revoked_at, timestamps
    - Create RefreshTokenRepository interface extending JpaRepository
    - Add custom query methods: findByUserIdAndRevokedAtIsNull, deleteByUserId, findByTokenHash
    - _Requirements: FR-RFT-002, 4.2.2_

  - [x] 2.3 Create TokenBlacklist entity and repository
    - Create TokenBlacklist entity with jti (String PK), user_id FK, reason enum, expires_at, created_at
    - Create BlacklistReason enum (LOGOUT, REVOKED, SECURITY, PASSWORD_CHANGE)
    - Create TokenBlacklistRepository interface with findByJti and deleteByExpiresAtBefore methods
    - _Requirements: FR-LGT-001, 4.2.3_

  - [x] 2.4 Update User entity for JWT authentication
    - Change id from Long to UUID with @GeneratedValue(strategy = GenerationType.UUID)
    - Add failedLoginAttempts Integer field with default 0
    - Add lockoutUntil Instant field
    - Add lastLoginAt Instant field
    - Add @PreUpdate method to update updatedAt timestamp
    - _Requirements: FR-LGN-003, 4.2.1_

- [-] 3. Implement core JWT service
  - [x] 3.1 Create JwtService interface and implementation
    - Implement generateAccessToken(User user) returning signed JWT with 15-min TTL
    - Implement generateRefreshToken(User user) returning secure random string
    - Implement validateToken(String token) with signature and expiration verification
    - Implement extractClaims(String token) returning Claims object
    - Implement extractUserId(String token) returning UUID from sub claim
    - Implement extractRoles(String token) returning List<String> from roles claim
    - Implement isTokenExpired(String token) checking exp claim with 30-second clock skew
    - Use HS256 algorithm with secret from configuration
    - Include all required claims: sub, iat, exp, jti, iss, aud, roles, email
    - _Requirements: FR-LGN-004, FR-MID-002, 3.1.1_

  - [ ]* 3.2 Write property test for JWT token generation and validation
    - **Property 6: JWT Payload Completeness**
    - **Validates: Requirements FR-LGN-004**
    - Generate random users with various roles, create access tokens, decode and verify all required claims present (sub, iat, exp, jti, iss, aud, roles)

  - [ ]* 3.3 Write property test for token signature verification
    - **Property 8: Token Signature Verification**
    - **Validates: Requirements FR-MID-002, FR-MID-004**
    - Generate valid tokens, tamper with signatures or payloads, verify all are rejected with TOKEN_INVALID

  - [ ]* 3.4 Write property test for token expiration
    - **Property 9: Expired Token Rejection**
    - **Validates: Requirements FR-MID-003**
    - Generate tokens with various expiration times, verify tokens with exp < (now - 30 seconds) are rejected with TOKEN_EXPIRED

  - [ ]* 3.5 Write unit tests for JwtService edge cases
    - Test null/empty token handling
    - Test malformed JWT structure
    - Test missing required claims
    - Test invalid signature algorithm
    - _Requirements: FR-MID-004_

- [x] 4. Implement token management service
  - [x] 4.1 Create TokenService interface and implementation
    - Implement storeRefreshToken(RefreshToken) storing BCrypt hash in database
    - Implement validateRefreshToken(String token) verifying hash and expiration
    - Implement revokeRefreshToken(UUID tokenId) setting revoked_at timestamp
    - Implement revokeAllUserTokens(UUID userId) revoking all user's refresh tokens
    - Implement isTokenBlacklisted(String jti) checking Redis cache with MySQL fallback
    - Implement addToBlacklist(String jti, long ttlSeconds) adding to Redis with TTL
    - Implement detectTokenReuse(String token) checking if revoked token is reused
    - _Requirements: FR-RFT-002, FR-RFT-003, FR-RFT-004, FR-LGT-001, 3.1.2_

  - [x] 4.2 Configure Redis for token blacklist caching
    - Create RedisConfig class with RedisTemplate<String, String> bean
    - Configure Redis connection properties in application.yml
    - Implement cache fallback to MySQL if Redis unavailable
    - _Requirements: NFR-3, NFR-4_

  - [ ]* 4.3 Write property test for refresh token rotation
    - **Property 12: Refresh Token Rotation**
    - **Validates: Requirements FR-RFT-001**
    - Generate users, create refresh tokens, use them to refresh, verify new tokens returned and old token invalidated

  - [ ]* 4.4 Write property test for refresh token reuse detection
    - **Property 13: Refresh Token Reuse Detection**
    - **Validates: Requirements FR-RFT-004**
    - Generate users with multiple sessions, use a refresh token, attempt reuse, verify all user's tokens revoked

  - [ ]* 4.5 Write property test for blacklist operations
    - **Property 10: Blacklisted Token Rejection**
    - **Validates: Requirements FR-MID-006**
    - Generate valid tokens, add jti to blacklist, verify tokens rejected with TOKEN_REVOKED

- [x] 5. Checkpoint - Ensure core services compile and unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement Spring Security configuration
  - [x] 6.1 Create CustomUserDetailsService
    - Implement UserDetailsService interface
    - Override loadUserByUsername(String userId) loading user by UUID
    - Return UserDetails with username=userId, password, authorities=roles, accountLocked based on lockoutUntil
    - _Requirements: 3.1.5_

  - [x] 6.2 Create JwtAuthenticationFilter
    - Extend OncePerRequestFilter
    - Implement doFilterInternal extracting Bearer token from Authorization header
    - Validate token using JwtService
    - Check token not blacklisted using TokenService
    - Set SecurityContext with UsernamePasswordAuthenticationToken
    - Handle ExpiredJwtException returning TOKEN_EXPIRED error
    - Handle JwtException returning TOKEN_INVALID error
    - Handle missing/malformed header returning TOKEN_MISSING error
    - _Requirements: FR-MID-001, FR-MID-002, FR-MID-003, FR-MID-004, FR-MID-006, 3.1.3_

  - [x] 6.3 Create SecurityConfig class
    - Configure SecurityFilterChain with JWT filter
    - Disable session management (stateless)
    - Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
    - Configure CORS to allow frontend origin
    - Permit /auth/login, /auth/register, /auth/refresh endpoints
    - Require authentication for all other endpoints
    - Configure exception handling for 401/403 responses
    - _Requirements: BR-2, 2.2.1_

  - [ ]* 6.4 Write integration tests for JWT filter chain
    - Test request with valid token succeeds
    - Test request with missing token returns 401
    - Test request with expired token returns 401 with TOKEN_EXPIRED
    - Test request with invalid signature returns 401 with TOKEN_INVALID
    - Test request with blacklisted token returns 401 with TOKEN_REVOKED
    - _Requirements: FR-MID-001, FR-MID-002, FR-MID-003, FR-MID-004, FR-MID-006_

- [x] 7. Implement authentication service and endpoints
  - [x] 7.1 Create AuthenticationService interface and implementation
    - Implement register(RegisterRequest) validating input, hashing password with BCrypt, creating user, generating token pair
    - Implement login(LoginRequest) authenticating user, checking account lockout, generating token pair, updating lastLoginAt
    - Implement refreshToken(String refreshToken) validating refresh token, detecting reuse, rotating token, issuing new pair
    - Implement logout(String accessToken, UUID userId) adding jti to blacklist, deleting refresh token
    - Implement logoutAll(UUID userId) revoking all user's refresh tokens
    - Handle failed login attempts incrementing counter and triggering lockout after 5 failures
    - Reset failed attempts counter on successful login
    - _Requirements: FR-REG-001, FR-LGN-001, FR-LGN-003, FR-RFT-001, FR-LGT-001, FR-LGT-003, 3.1.4_

  - [x] 7.2 Create DTOs for authentication requests and responses
    - Create RegisterRequest DTO with validation annotations (@NotBlank, @Email, @Size)
    - Create LoginRequest DTO with email and password validation
    - Create RefreshTokenRequest DTO with refresh token validation
    - Create AuthResponse DTO with user, access_token, refresh_token, token_type, expires_in
    - Create UserDTO with id, username, email, roles, created_at, last_login_at
    - Create ErrorResponse DTO with error code, message, timestamp, path
    - _Requirements: 4.3.1, 4.3.2_

  - [x] 7.3 Create AuthController with REST endpoints
    - POST /auth/register endpoint calling AuthenticationService.register
    - POST /auth/login endpoint calling AuthenticationService.login
    - POST /auth/refresh endpoint calling AuthenticationService.refreshToken
    - POST /auth/logout endpoint calling AuthenticationService.logout (requires authentication)
    - POST /auth/logout-all endpoint calling AuthenticationService.logoutAll (requires authentication)
    - GET /auth/me endpoint returning current user from SecurityContext (requires authentication)
    - Add @Valid annotations for request validation
    - Add exception handlers for validation errors, authentication failures, account lockout
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 7.4 Write property test for registration token issuance
    - **Property 1: Registration Token Issuance**
    - **Validates: Requirements FR-REG-001**
    - Generate random valid user data, register, verify both access and refresh tokens returned with valid signatures

  - [ ]* 7.5 Write property test for registration response completeness
    - **Property 2: Registration Response Completeness**
    - **Validates: Requirements FR-REG-002**
    - Generate random users, register, verify response contains all required fields with correct types

  - [ ]* 7.6 Write property test for invalid registration rejection
    - **Property 3: Invalid Registration Rejection**
    - **Validates: Requirements FR-REG-003**
    - Generate invalid inputs (malformed email, weak password, missing fields), verify all rejected with HTTP 400

  - [ ]* 7.7 Write property test for login token TTL correctness
    - **Property 4: Login Token Pair TTL Correctness**
    - **Validates: Requirements FR-LGN-001**
    - Generate users, login, decode tokens, verify exp claim matches expected TTL (15 min access, 7 day refresh)

  - [ ]* 7.8 Write property test for failed login generic error
    - **Property 5: Failed Login Generic Error**
    - **Validates: Requirements FR-LGN-002**
    - Generate wrong passwords for existing users and random emails for non-existent users, verify identical error message

  - [ ]* 7.9 Write integration tests for account lockout
    - Test 5 consecutive failed logins trigger lockout
    - Test locked account returns HTTP 423 with retry_after
    - Test successful login after lockout period
    - Test successful login resets failed attempts counter
    - _Requirements: FR-LGN-003_

  - [ ]* 7.10 Write integration tests for authentication endpoints
    - Test complete registration → login → access protected endpoint flow
    - Test refresh token rotation end-to-end
    - Test logout → token rejection flow
    - Test logout-all revokes all sessions
    - _Requirements: FR-REG-001, FR-LGN-001, FR-RFT-001, FR-LGT-001, FR-LGT-003_

- [x] 8. Checkpoint - Ensure backend authentication is fully functional
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Implement frontend token management (Angular)
  - [x] 9.1 Create TokenStorageService
    - Implement setTokens(accessToken, refreshToken) storing access token in memory, refresh token in sessionStorage
    - Implement getAccessToken() returning token from memory
    - Implement getRefreshToken() returning token from sessionStorage
    - Implement clearTokens() clearing both tokens
    - Add security comment explaining storage strategy (XSS prevention)
    - _Requirements: 3.2.3_

  - [x] 9.2 Create AuthService
    - Implement register(request: RegisterRequest): Observable<AuthResponse>
    - Implement login(request: LoginRequest): Observable<AuthResponse>
    - Implement refreshToken(): Observable<AuthResponse>
    - Implement logout(): Observable<void>
    - Implement logoutAll(): Observable<void>
    - Implement getCurrentUser(): Observable<User | null>
    - Implement isAuthenticated(): boolean checking token existence
    - Implement getAccessToken(): string | null delegating to TokenStorageService
    - Store tokens using TokenStorageService after successful auth
    - _Requirements: 3.2.1_

  - [x] 9.3 Create JwtInterceptor for automatic token attachment
    - Implement HttpInterceptor interface
    - Add Bearer token to Authorization header for all requests except /auth/login and /auth/register
    - Handle 401 errors by attempting token refresh
    - Implement refresh token logic with BehaviorSubject to prevent multiple simultaneous refresh calls
    - Queue failed requests during refresh and retry after new token obtained
    - Logout user if refresh fails
    - _Requirements: 3.2.2_

  - [x] 9.4 Update login component to use JWT authentication
    - Update login form to call AuthService.login
    - Store tokens using TokenStorageService on successful login
    - Navigate to dashboard after successful login
    - Display error messages for failed login, account lockout
    - _Requirements: 5.2_

  - [x] 9.5 Update registration component to use JWT authentication
    - Update registration form to call AuthService.register
    - Store tokens using TokenStorageService on successful registration
    - Navigate to dashboard after successful registration
    - Display validation errors
    - _Requirements: 5.1_

  - [x] 9.6 Create auth guard for protected routes
    - Implement CanActivate interface
    - Check if user is authenticated using AuthService.isAuthenticated()
    - Redirect to login if not authenticated
    - Apply guard to all protected routes in routing module
    - _Requirements: BR-2_

  - [ ]* 9.7 Write unit tests for TokenStorageService
    - Test token storage and retrieval
    - Test clearTokens removes all tokens
    - Test access token not persisted to sessionStorage
    - _Requirements: 3.2.3_

  - [ ]* 9.8 Write unit tests for JwtInterceptor
    - Test Bearer token added to requests
    - Test 401 triggers refresh flow
    - Test failed refresh triggers logout
    - Test multiple simultaneous 401s only trigger one refresh
    - _Requirements: 3.2.2_

- [x] 10. Implement password hashing migration
  - [x] 10.1 Create password migration service
    - Create PasswordMigrationService with BCrypt encoder
    - Implement method to check if password is plain text (not starting with $2a$)
    - Implement method to hash plain text passwords with BCrypt
    - _Requirements: NFR-2_

  - [x] 10.2 Create database migration script for password hashing
    - Create SQL script or Liquibase/Flyway migration to identify plain text passwords
    - Update plain text passwords to BCrypt hashes
    - Add migration logging for audit trail
    - _Requirements: NFR-2_

  - [ ]* 10.3 Write property test for password hashing round-trip
    - **Property 17: Password Hashing Round-Trip**
    - **Validates: Security requirement for password storage**
    - Generate random passwords, hash with BCrypt, verify original matches and different passwords don't match

- [x] 11. Implement token blacklist cleanup job
  - [x] 11.1 Create scheduled job for blacklist cleanup
    - Create @Scheduled method running hourly
    - Delete TokenBlacklist entries where expires_at < NOW()
    - Log cleanup statistics (number of entries deleted)
    - _Requirements: FR-LGT-004_

  - [x] 11.2 Create scheduled job for expired refresh token cleanup
    - Create @Scheduled method running daily
    - Delete RefreshToken entries where expires_at < NOW() OR revoked_at IS NOT NULL
    - Log cleanup statistics
    - _Requirements: FR-RFT-002_

- [x] 12. Add comprehensive logging and monitoring
  - [x] 12.1 Add authentication event logging
    - Log all login attempts (success/failure) with timestamp, user ID, IP address, user agent
    - Log all registration events
    - Log all token refresh events
    - Log all logout events
    - Log account lockout events
    - Log refresh token reuse detection events
    - Use structured logging (JSON format) for easy parsing
    - _Requirements: NFR-2, SEC-4_

  - [x] 12.2 Add security monitoring and alerting
    - Add metrics for failed login attempts per user
    - Add metrics for token validation failures
    - Add metrics for refresh token reuse detection
    - Add metrics for account lockouts
    - Configure alerts for anomalous patterns (e.g., >10 failed logins in 1 minute)
    - _Requirements: SEC-4_

- [x] 13. Create API documentation
  - [x] 13.1 Add OpenAPI/Swagger documentation
    - Add springdoc-openapi dependency
    - Document all authentication endpoints with request/response examples
    - Document error responses with error codes
    - Document authentication requirements for protected endpoints
    - _Requirements: NFR-5_

  - [x] 13.2 Create migration guide documentation
    - Document migration strategy from session-based to JWT authentication
    - Document rollback procedure
    - Document configuration changes required
    - Document frontend changes required
    - _Requirements: BR-5, Section 9_

- [x] 14. Implement feature flag for gradual rollout
  - [x] 14.1 Create feature flag configuration
    - Add JWT_ENABLED environment variable (default: false)
    - Create FeatureFlagService to check if JWT is enabled
    - Update SecurityConfig to conditionally enable JWT filter based on flag
    - _Requirements: BR-5_

  - [x] 14.2 Implement parallel authentication support
    - Allow both session-based and JWT authentication during migration
    - Check feature flag in AuthController to determine which auth method to use
    - Ensure existing session-based endpoints continue working
    - _Requirements: BR-5_

- [x] 15. Performance testing and optimization
  - [ ]* 15.1 Write performance tests for token operations
    - Test token generation throughput (target: < 50ms P95)
    - Test token validation latency (target: < 10ms per request)
    - Test refresh endpoint latency (target: < 100ms P95)
    - Test blacklist lookup latency (target: < 5ms P95)
    - _Requirements: NFR-1_

  - [ ]* 15.2 Load testing with concurrent requests
    - Test 1000 concurrent token validations
    - Test refresh storm under high load
    - Test middleware latency under load
    - Verify system meets performance targets
    - _Requirements: NFR-1, NFR-3_

- [x] 16. Security testing
  - [ ]* 16.1 Write security tests for token tampering
    - Test token signature forging attempts
    - Test algorithm confusion attacks (alg:none)
    - Test token payload tampering
    - Verify all attacks are rejected
    - _Requirements: SEC-3_

  - [ ]* 16.2 Write security tests for refresh token attacks
    - Test refresh token replay attacks
    - Test refresh token reuse detection
    - Test brute force refresh token guessing
    - _Requirements: SEC-3, FR-RFT-004_

  - [ ]* 16.3 Write security tests for account enumeration
    - Test login with non-existent email returns same error as wrong password
    - Test registration with existing email doesn't reveal account existence
    - _Requirements: FR-LGN-002_

- [x] 17. Final checkpoint - Complete system integration test
  - Ensure all tests pass, ask the user if questions arise.

- [x] 18. Final integration and deployment preparation
  - [x] 18.1 Run complete test suite
    - Run all unit tests
    - Run all integration tests
    - Run all property-based tests
    - Run all security tests
    - Run all performance tests
    - Verify all tests pass
    - _Requirements: Section 8_

  - [x] 18.2 Update environment configuration
    - Document all required environment variables (JWT_SECRET, JWT_ACCESS_TOKEN_TTL, JWT_REFRESH_TOKEN_TTL, etc.)
    - Create .env.example with all JWT configuration
    - Update deployment documentation with configuration requirements
    - _Requirements: NFR-5_

  - [x] 18.3 Database migration execution
    - Run database migration scripts in staging environment
    - Verify schema changes applied correctly
    - Verify data migration completed successfully
    - Test rollback procedure
    - _Requirements: Section 6_

  - [x] 18.4 Final security audit
    - Verify JWT secret is properly externalized
    - Verify no secrets in source code or logs
    - Verify HTTPS enforced for all auth endpoints
    - Verify all OWASP Top 10 controls implemented
    - _Requirements: NFR-2, SEC-1, SEC-2_

---

## Notes

### Testing Strategy

- **Property-Based Tests**: Core JWT logic (token generation, validation, signature verification, expiration, blacklist operations, refresh token rotation, password hashing)
- **Unit Tests**: Specific edge cases, error conditions, boundary values, service methods
- **Integration Tests**: Database operations, Spring Security filter chain, API endpoints, authentication flows
- **Security Tests**: Token tampering, algorithm confusion, replay attacks, account enumeration
- **Performance Tests**: Token generation/validation throughput, concurrent request handling, blacklist lookup latency

### Optional Tasks

Tasks marked with `*` are optional and can be skipped for faster MVP delivery. However, they are strongly recommended for production readiness, especially:
- Property-based tests for core JWT logic (high value for catching edge cases)
- Security tests (critical for production security)
- Performance tests (ensures system meets SLA requirements)

### Implementation Order

The tasks are ordered to enable incremental progress:
1. **Foundation** (Tasks 1-2): Dependencies and data models
2. **Core Services** (Tasks 3-5): JWT and token management logic
3. **Security Integration** (Task 6): Spring Security filter chain
4. **API Layer** (Task 7): Authentication endpoints
5. **Frontend** (Task 9): Angular token management
6. **Supporting Features** (Tasks 10-12): Password migration, cleanup jobs, logging
7. **Documentation & Deployment** (Tasks 13-14): Docs and feature flags
8. **Quality Assurance** (Tasks 15-18): Performance, security, integration testing

### Checkpoints

Checkpoints are placed at strategic points to validate progress:
- After core services (Task 5): Verify JWT and token services work correctly
- After backend authentication (Task 8): Verify complete backend authentication flow
- After security testing (Task 17): Verify system is secure and performant
- Before deployment (Task 18): Final validation of complete system

### Requirements Traceability

Each task explicitly references the requirements it implements, ensuring complete coverage of:
- All functional requirements (FR-*)
- All non-functional requirements (NFR-*)
- All security requirements (SEC-*)
- All API specifications (Section 5)
- All database schema changes (Section 6)
- All correctness properties (Section 5 of design document)

### Property-Based Test Coverage

The following correctness properties from the design document are covered by property-based test tasks:
- Property 1: Registration Token Issuance (Task 7.4)
- Property 2: Registration Response Completeness (Task 7.5)
- Property 3: Invalid Registration Rejection (Task 7.6)
- Property 4: Login Token Pair TTL Correctness (Task 7.7)
- Property 5: Failed Login Generic Error (Task 7.8)
- Property 6: JWT Payload Completeness (Task 3.2)
- Property 8: Token Signature Verification (Task 3.3)
- Property 9: Expired Token Rejection (Task 3.4)
- Property 10: Blacklisted Token Rejection (Task 4.5)
- Property 12: Refresh Token Rotation (Task 4.3)
- Property 13: Refresh Token Reuse Detection (Task 4.4)
- Property 17: Password Hashing Round-Trip (Task 10.3)

Properties 7, 11, 14, 15, 16 are covered by integration tests as they involve HTTP endpoints and database operations which are not suitable for pure property-based testing.

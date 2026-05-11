package aptms.services.impl;

import aptms.config.properties.JwtConfigProperties;
import aptms.dto.AuthResponse;
import aptms.dto.LoginRequest;
import aptms.dto.RegisterRequest;
import aptms.dto.UserDTO;
import aptms.entities.RefreshToken;
import aptms.entities.User;
import aptms.enums.BlacklistReason;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.RefreshTokenRepository;
import aptms.repositories.UserRepository;
import aptms.services.AuthenticationEventLogger;
import aptms.services.AuthenticationService;
import aptms.services.JwtService;
import aptms.services.SecurityMetricsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.SecurityConstants.*;
import static aptms.constants.ValidationConstants.*;

/**
 * Implementation of authentication service with JWT token management.
 * 
 * Provides secure authentication with:
 * - BCrypt password hashing
 * - JWT token generation and validation
 * - Refresh token rotation
 * - Account lockout after failed attempts
 * - Token blacklist management
 * 
 * Requirements: FR-REG-001, FR-LGN-001, FR-LGN-003, FR-RFT-001, FR-LGT-001, FR-LGT-003, 3.1.4
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;
    private static final int BCRYPT_STRENGTH = 10;
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final TokenServiceImpl tokenService;
    private final JwtConfigProperties jwtConfig;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationEventLogger eventLogger;
    private final SecurityMetricsService metricsService;
    
    public AuthenticationServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            TokenServiceImpl tokenService,
            JwtConfigProperties jwtConfig,
            AuthenticationEventLogger eventLogger,
            SecurityMetricsService metricsService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
        this.eventLogger = eventLogger;
        this.metricsService = metricsService;
        
        logger.info("AuthenticationService initialized");
    }
    
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registration attempt for email: {}", request.getEmail());
        
        // Capture request context
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed: email already exists: {}", request.getEmail());
            eventLogger.logRegistrationFailure(request.getEmail(), ipAddress, userAgent, "Email already exists");
            throw new DuplicateValueFoundExceptions(
                String.format(DUPLICATE_ENTRY_MESSAGE, FIELD_EMAIL)
            );
        }
        
        // Create user entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setCountryId(request.getCountryId());
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        
        // Save user
        user = userRepository.save(user);
        logger.info("User registered successfully: {} ({})", user.getId(), user.getEmail());
        
        // Log registration success event
        eventLogger.logRegistrationSuccess(user.getId(), user.getEmail(), ipAddress, userAgent);
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user);
        
        // Store refresh token
        RefreshToken refreshToken = createRefreshToken(user, refreshTokenString);
        tokenService.storeRefreshToken(refreshToken);
        
        // Build response
        UserDTO userDTO = buildUserDTO(user);
        
        logger.info("Registration completed for user: {}", user.getId());
        
        return AuthResponse.builder()
            .user(userDTO)
            .accessToken(accessToken)
            .refreshToken(refreshTokenString)
            .tokenType("Bearer")
            .expiresIn(jwtConfig.getAccessTokenTtlSeconds())
            .build();
    }
    
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());
        
        // Capture request context
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> {
                logger.warn("Login failed: user not found: {}", request.getEmail());
                eventLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Invalid credentials");
                metricsService.recordFailedLogin(request.getEmail(), "user_not_found");
                return new InvalidException(INVALID_CREDENTIALS_MESSAGE);
            });
        
        // Check if account is locked
        if (user.isLocked()) {
            logger.warn("Login failed: account locked: {} (lockout until: {})", 
                user.getId(), user.getLockoutUntil());
            eventLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Account locked");
            metricsService.recordFailedLogin(request.getEmail(), "account_locked");
            throw new InvalidException(
                String.format("Account is locked due to too many failed login attempts. " +
                    "Please try again after %s", user.getLockoutUntil())
            );
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Increment failed attempts
            handleFailedLogin(user, ipAddress, userAgent);
            logger.warn("Login failed: invalid password for user: {}", user.getId());
            eventLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Invalid credentials");
            metricsService.recordFailedLogin(request.getEmail(), "invalid_password");
            throw new InvalidException(INVALID_CREDENTIALS_MESSAGE);
        }
        
        // Successful login - reset failed attempts
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
        }
        
        // Update last login timestamp
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        
        logger.info("Login successful for user: {}", user.getId());
        
        // Log login success event and record metric
        eventLogger.logLoginSuccess(user.getId(), user.getEmail(), ipAddress, userAgent);
        metricsService.recordSuccessfulLogin(user.getId());
        
        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenString = jwtService.generateRefreshToken(user);
        
        // Store refresh token
        RefreshToken refreshToken = createRefreshToken(user, refreshTokenString);
        tokenService.storeRefreshToken(refreshToken);
        
        // Build response
        UserDTO userDTO = buildUserDTO(user);
        
        return AuthResponse.builder()
            .user(userDTO)
            .accessToken(accessToken)
            .refreshToken(refreshTokenString)
            .tokenType("Bearer")
            .expiresIn(jwtConfig.getAccessTokenTtlSeconds())
            .build();
    }
    
    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshTokenString) {
        logger.info("Token refresh attempt");
        
        // Capture request context
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        
        // Detect token reuse (security event)
        if (tokenService.detectTokenReuse(refreshTokenString)) {
            logger.error("SECURITY: Refresh token reuse detected!");
            
            // Find the token to get the user
            RefreshToken token = findRefreshTokenByString(refreshTokenString);
            if (token != null) {
                // Log security event and record metric
                eventLogger.logRefreshTokenReuse(token.getUser().getId(), ipAddress, userAgent);
                metricsService.recordRefreshTokenReuse(token.getUser().getId());
                
                // Revoke ALL user's tokens
                tokenService.revokeAllUserTokens(token.getUser().getId());
                logger.error("SECURITY: All tokens revoked for user: {} due to token reuse", 
                    token.getUser().getId());
            }
            
            throw new InvalidException("Refresh token reuse detected. All sessions have been terminated for security.");
        }
        
        // Validate refresh token
        RefreshToken refreshToken = tokenService.validateRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();
        
        logger.info("Refresh token validated for user: {}", user.getId());
        
        // Log token refresh event
        eventLogger.logTokenRefresh(user.getId(), ipAddress, userAgent);
        
        // Revoke old refresh token (rotation)
        tokenService.revokeRefreshToken(refreshToken.getId());
        
        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenString = jwtService.generateRefreshToken(user);
        
        // Store new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user, newRefreshTokenString);
        tokenService.storeRefreshToken(newRefreshToken);
        
        logger.info("Token refresh completed for user: {}", user.getId());
        
        // Build response
        UserDTO userDTO = buildUserDTO(user);
        
        return AuthResponse.builder()
            .user(userDTO)
            .accessToken(newAccessToken)
            .refreshToken(newRefreshTokenString)
            .tokenType("Bearer")
            .expiresIn(jwtConfig.getAccessTokenTtlSeconds())
            .build();
    }
    
    @Override
    @Transactional
    public void logout(String accessToken, UUID userId) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token cannot be null or blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        logger.info("Logout attempt for user: {}", userId);
        
        // Capture request context
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        
        // Extract jti from access token
        Claims claims = jwtService.extractClaims(accessToken);
        String jti = claims.getId();
        
        // Calculate remaining TTL for blacklist
        Instant expiration = claims.getExpiration().toInstant();
        long ttlSeconds = Duration.between(Instant.now(), expiration).getSeconds();
        
        if (ttlSeconds > 0) {
            // Add to blacklist
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidException("User not found: " + userId));
            
            tokenService.addToBlacklist(jti, user, BlacklistReason.LOGOUT, ttlSeconds);
        }
        
        // Delete user's refresh tokens
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        if (!userTokens.isEmpty()) {
            Instant now = Instant.now();
            for (RefreshToken token : userTokens) {
                token.setRevokedAt(now);
            }
            refreshTokenRepository.saveAll(userTokens);
        }
        
        // Log logout event
        eventLogger.logLogout(userId, ipAddress, userAgent, false);
        
        logger.info("Logout completed for user: {}", userId);
    }
    
    @Override
    @Transactional
    public void logoutAll(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        logger.info("Logout all sessions for user: {}", userId);
        
        // Capture request context
        String ipAddress = getClientIpAddress();
        String userAgent = getUserAgent();
        
        // Revoke all refresh tokens
        tokenService.revokeAllUserTokens(userId);
        
        // Log logout all event
        eventLogger.logLogout(userId, ipAddress, userAgent, true);
        
        logger.info("All sessions terminated for user: {}", userId);
    }
    
    /**
     * Handle failed login attempt.
     * Increments failed attempts counter and triggers lockout after 5 failures.
     */
    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Lock account for 15 minutes
            Instant lockoutUntil = Instant.now().plus(Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
            user.setLockoutUntil(lockoutUntil);
            logger.warn("Account locked for user: {} (attempts: {}, lockout until: {})", 
                user.getId(), attempts, lockoutUntil);
            
            // Log account lockout event and record metric
            eventLogger.logAccountLockout(user.getId(), user.getEmail(), ipAddress, userAgent, 
                attempts, lockoutUntil.toString());
            metricsService.recordAccountLockout(user.getId(), attempts);
        } else {
            logger.debug("Failed login attempt {} for user: {}", attempts, user.getId());
        }
        
        userRepository.save(user);
    }
    
    /**
     * Create RefreshToken entity from token string.
     */
    private RefreshToken createRefreshToken(User user, String tokenString) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        // Use SHA-256 for refresh token hashing (BCrypt has 72-byte limit, refresh tokens are 86 chars)
        refreshToken.setTokenHash(hashRefreshToken(tokenString));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtConfig.getRefreshTokenTtl()));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setUpdatedAt(Instant.now());
        
        // Capture device info from request context
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                refreshToken.setIpAddress(getClientIpAddress(request));
                refreshToken.setUserAgent(request.getHeader("User-Agent"));
                refreshToken.setDeviceInfo(extractDeviceInfo(request.getHeader("User-Agent")));
            }
        } catch (Exception e) {
            logger.debug("Could not capture device info: {}", e.getMessage());
        }
        
        return refreshToken;
    }
    
    /**
     * Hash refresh token using SHA-256
     * BCrypt has a 72-byte limit, but refresh tokens are 86 characters (64 bytes base64-encoded)
     */
    private String hashRefreshToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Build UserDTO from User entity.
     */
    private UserDTO buildUserDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .roles(List.of(user.getRole().name()))
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
    
    /**
     * Find refresh token by token string (for reuse detection).
     */
    private RefreshToken findRefreshTokenByString(String tokenString) {
        List<RefreshToken> allTokens = refreshTokenRepository.findAll();
        for (RefreshToken rt : allTokens) {
            if (passwordEncoder.matches(tokenString, rt.getTokenHash())) {
                return rt;
            }
        }
        return null;
    }
    
    /**
     * Extract client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Get client IP address from current request context.
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getClientIpAddress(request);
            }
        } catch (Exception e) {
            logger.debug("Could not get IP address: {}", e.getMessage());
        }
        return "unknown";
    }
    
    /**
     * Get user agent from current request context.
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userAgent = request.getHeader("User-Agent");
                return userAgent != null ? userAgent : "unknown";
            }
        } catch (Exception e) {
            logger.debug("Could not get user agent: {}", e.getMessage());
        }
        return "unknown";
    }
    
    /**
     * Extract device info from User-Agent header.
     */
    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }
        
        // Simple device detection
        if (userAgent.contains("Mobile")) {
            return "Mobile";
        } else if (userAgent.contains("Tablet")) {
            return "Tablet";
        } else {
            return "Desktop";
        }
    }
}

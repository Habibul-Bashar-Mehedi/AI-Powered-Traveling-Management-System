package aptms.services;

import java.util.UUID;

/**
 * Service for tracking security-related metrics using Micrometer.
 * 
 * Metrics tracked:
 * - Failed login attempts per user
 * - Token validation failures
 * - Refresh token reuse detection
 * - Account lockouts
 * 
 * Requirements: SEC-4
 */
public interface SecurityMetricsService {
    
    /**
     * Record a failed login attempt.
     * 
     * @param email User email
     * @param reason Failure reason
     */
    void recordFailedLogin(String email, String reason);
    
    /**
     * Record a successful login.
     * 
     * @param userId User ID
     */
    void recordSuccessfulLogin(UUID userId);
    
    /**
     * Record a token validation failure.
     * 
     * @param errorCode Error code (TOKEN_EXPIRED, TOKEN_INVALID, TOKEN_REVOKED)
     */
    void recordTokenValidationFailure(String errorCode);
    
    /**
     * Record a refresh token reuse detection (security incident).
     * 
     * @param userId User ID
     */
    void recordRefreshTokenReuse(UUID userId);
    
    /**
     * Record an account lockout.
     * 
     * @param userId User ID
     * @param failedAttempts Number of failed attempts
     */
    void recordAccountLockout(UUID userId, int failedAttempts);
    
    /**
     * Get the current count of failed login attempts for a user.
     * 
     * @param email User email
     * @return Count of failed attempts in the current time window
     */
    double getFailedLoginCount(String email);
}

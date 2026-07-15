package aptms.services;

import aptms.enums.AuthEventType;

import java.util.UUID;

/**
 * Service for logging authentication events with structured JSON format.
 * 
 * All authentication events are logged with:
 * - Timestamp (automatic)
 * - User ID
 * - IP address
 * - User agent
 * - Event type
 * - Success/failure status
 * - Additional context
 * 
 * Requirements: NFR-2, SEC-4
 */
public interface AuthenticationEventLogger {
    
    /**
     * Log a successful login event.
     * 
     * @param userId User ID
     * @param email User email
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     */
    void logLoginSuccess(UUID userId, String email, String ipAddress, String userAgent);
    
    /**
     * Log a failed login event.
     * 
     * @param email Attempted email
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param reason Failure reason
     */
    void logLoginFailure(String email, String ipAddress, String userAgent, String reason);
    
    /**
     * Log a successful registration event.
     * 
     * @param userId User ID
     * @param email User email
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     */
    void logRegistrationSuccess(UUID userId, String email, String ipAddress, String userAgent);
    
    /**
     * Log a failed registration event.
     * 
     * @param email Attempted email
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param reason Failure reason
     */
    void logRegistrationFailure(String email, String ipAddress, String userAgent, String reason);
    
    /**
     * Log a token refresh event.
     * 
     * @param userId User ID
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     */
    void logTokenRefresh(UUID userId, String ipAddress, String userAgent);
    
    /**
     * Log a logout event.
     * 
     * @param userId User ID
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param logoutAll Whether this is a logout-all event
     */
    void logLogout(UUID userId, String ipAddress, String userAgent, boolean logoutAll);
    
    /**
     * Log an account lockout event.
     * 
     * @param userId User ID
     * @param email User email
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param failedAttempts Number of failed attempts
     * @param lockoutUntil Lockout expiration timestamp
     */
    void logAccountLockout(UUID userId, String email, String ipAddress, String userAgent, 
                          int failedAttempts, String lockoutUntil);
    
    /**
     * Log a refresh token reuse detection event (security incident).
     * 
     * @param userId User ID
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     */
    void logRefreshTokenReuse(UUID userId, String ipAddress, String userAgent);
    
    /**
     * Log a token validation failure event.
     * 
     * @param ipAddress Client IP address
     * @param userAgent Client user agent
     * @param errorCode Error code (TOKEN_EXPIRED, TOKEN_INVALID, TOKEN_REVOKED)
     * @param path Request path
     */
    void logTokenValidationFailure(String ipAddress, String userAgent, String errorCode, String path);

    /**
     * Log that an OTP verification code was sent (registration or resend).
     * Never includes the OTP value itself.
     */
    void logOtpSent(String email, String ipAddress, String userAgent);

    /**
     * Log a successful OTP verification (account activated).
     */
    void logOtpVerificationSuccess(UUID userId, String email, String ipAddress, String userAgent);

    /**
     * Log a failed OTP verification attempt.
     *
     * @param reason Failure reason (e.g. "invalid_code", "expired", "max_attempts") — never the OTP value.
     */
    void logOtpVerificationFailure(String email, String ipAddress, String userAgent, String reason);

    /**
     * Log an OTP resend request.
     */
    void logOtpResent(String email, String ipAddress, String userAgent);
}

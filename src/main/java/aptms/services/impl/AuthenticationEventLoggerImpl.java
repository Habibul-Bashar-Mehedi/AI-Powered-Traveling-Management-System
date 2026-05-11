package aptms.services.impl;

import aptms.enums.AuthEventType;
import aptms.services.AuthenticationEventLogger;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementation of authentication event logger with structured JSON logging.
 * 
 * Uses Logstash Logback Encoder for structured logging that can be easily
 * parsed by log aggregation tools (ELK, Splunk, etc.).
 * 
 * All events are logged with:
 * - timestamp (automatic from Logback)
 * - event_type
 * - user_id (when available)
 * - email (when available)
 * - ip_address
 * - user_agent
 * - success (boolean)
 * - Additional context fields
 * 
 * Requirements: NFR-2, SEC-4
 */
@Service
public class AuthenticationEventLoggerImpl implements AuthenticationEventLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("AUTH_EVENTS");
    
    @Override
    public void logLoginSuccess(UUID userId, String email, String ipAddress, String userAgent) {
        logger.info("Login successful",
            StructuredArguments.keyValue("event_type", AuthEventType.LOGIN_SUCCESS),
            StructuredArguments.keyValue("user_id", userId),
            StructuredArguments.keyValue("email", email),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("success", true),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logLoginFailure(String email, String ipAddress, String userAgent, String reason) {
        logger.warn("Login failed",
            StructuredArguments.keyValue("event_type", AuthEventType.LOGIN_FAILURE),
            StructuredArguments.keyValue("email", email),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("success", false),
            StructuredArguments.keyValue("reason", reason),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logRegistrationSuccess(UUID userId, String email, String ipAddress, String userAgent) {
        logger.info("Registration successful",
            StructuredArguments.keyValue("event_type", AuthEventType.REGISTRATION_SUCCESS),
            StructuredArguments.keyValue("user_id", userId),
            StructuredArguments.keyValue("email", email),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("success", true),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logRegistrationFailure(String email, String ipAddress, String userAgent, String reason) {
        logger.warn("Registration failed",
            StructuredArguments.keyValue("event_type", AuthEventType.REGISTRATION_FAILURE),
            StructuredArguments.keyValue("email", email),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("success", false),
            StructuredArguments.keyValue("reason", reason),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logTokenRefresh(UUID userId, String ipAddress, String userAgent) {
        logger.info("Token refresh",
            StructuredArguments.keyValue("event_type", AuthEventType.TOKEN_REFRESH),
            StructuredArguments.keyValue("user_id", userId),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("success", true),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logLogout(UUID userId, String ipAddress, String userAgent, boolean logoutAll) {
        AuthEventType eventType = logoutAll ? AuthEventType.LOGOUT_ALL : AuthEventType.LOGOUT;
        
        logger.info("Logout",
            StructuredArguments.keyValue("event_type", eventType),
            StructuredArguments.keyValue("user_id", userId),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("logout_all", logoutAll),
            StructuredArguments.keyValue("success", true),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logAccountLockout(UUID userId, String email, String ipAddress, String userAgent, 
                                  int failedAttempts, String lockoutUntil) {
        logger.warn("Account locked",
            StructuredArguments.keyValue("event_type", AuthEventType.ACCOUNT_LOCKOUT),
            StructuredArguments.keyValue("user_id", userId),
            StructuredArguments.keyValue("email", email),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("failed_attempts", failedAttempts),
            StructuredArguments.keyValue("lockout_until", lockoutUntil),
            StructuredArguments.keyValue("success", false),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logRefreshTokenReuse(UUID userId, String ipAddress, String userAgent) {
        logger.error("SECURITY ALERT: Refresh token reuse detected",
            StructuredArguments.keyValue("event_type", AuthEventType.REFRESH_TOKEN_REUSE),
            StructuredArguments.keyValue("user_id", userId),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("security_incident", true),
            StructuredArguments.keyValue("success", false),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
    
    @Override
    public void logTokenValidationFailure(String ipAddress, String userAgent, String errorCode, String path) {
        logger.warn("Token validation failed",
            StructuredArguments.keyValue("event_type", AuthEventType.TOKEN_VALIDATION_FAILURE),
            StructuredArguments.keyValue("ip_address", ipAddress),
            StructuredArguments.keyValue("user_agent", userAgent),
            StructuredArguments.keyValue("error_code", errorCode),
            StructuredArguments.keyValue("path", path),
            StructuredArguments.keyValue("success", false),
            StructuredArguments.keyValue("timestamp", Instant.now().toString())
        );
    }
}

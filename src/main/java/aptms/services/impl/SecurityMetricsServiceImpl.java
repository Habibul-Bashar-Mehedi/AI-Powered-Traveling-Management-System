package aptms.services.impl;

import aptms.services.SecurityMetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of security metrics service using Micrometer.
 * 
 * Provides metrics for:
 * - Failed login attempts (counter with email tag)
 * - Successful logins (counter)
 * - Token validation failures (counter with error_code tag)
 * - Refresh token reuse (counter)
 * - Account lockouts (counter)
 * 
 * Metrics are exposed via Spring Boot Actuator and can be scraped by Prometheus.
 * 
 * Requirements: SEC-4
 */
@Service
public class SecurityMetricsServiceImpl implements SecurityMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityMetricsServiceImpl.class);
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> failedLoginCounts;
    
    public SecurityMetricsServiceImpl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.failedLoginCounts = new ConcurrentHashMap<>();
        
        logger.info("SecurityMetricsService initialized with MeterRegistry");
    }
    
    @Override
    public void recordFailedLogin(String email, String reason) {
        // Increment counter with tags
        Counter.builder("auth.login.failed")
            .description("Number of failed login attempts")
            .tags(Arrays.asList(
                Tag.of("email", sanitizeEmail(email)),
                Tag.of("reason", reason)
            ))
            .register(meterRegistry)
            .increment();
        
        // Track in-memory count for anomaly detection
        failedLoginCounts.computeIfAbsent(email, k -> new AtomicInteger(0)).incrementAndGet();
        
        logger.debug("Recorded failed login for email: {} (reason: {})", email, reason);
    }
    
    @Override
    public void recordSuccessfulLogin(UUID userId) {
        Counter.builder("auth.login.success")
            .description("Number of successful login attempts")
            .tag("user_id", userId.toString())
            .register(meterRegistry)
            .increment();
        
        logger.debug("Recorded successful login for user: {}", userId);
    }
    
    @Override
    public void recordTokenValidationFailure(String errorCode) {
        Counter.builder("auth.token.validation.failed")
            .description("Number of token validation failures")
            .tag("error_code", errorCode)
            .register(meterRegistry)
            .increment();
        
        logger.debug("Recorded token validation failure: {}", errorCode);
    }
    
    @Override
    public void recordRefreshTokenReuse(UUID userId) {
        Counter.builder("auth.refresh_token.reuse")
            .description("Number of refresh token reuse detections (security incidents)")
            .tag("user_id", userId.toString())
            .register(meterRegistry)
            .increment();
        
        logger.warn("Recorded refresh token reuse for user: {}", userId);
    }
    
    @Override
    public void recordAccountLockout(UUID userId, int failedAttempts) {
        Counter.builder("auth.account.lockout")
            .description("Number of account lockouts")
            .tags(Arrays.asList(
                Tag.of("user_id", userId.toString()),
                Tag.of("failed_attempts", String.valueOf(failedAttempts))
            ))
            .register(meterRegistry)
            .increment();
        
        logger.warn("Recorded account lockout for user: {} (attempts: {})", userId, failedAttempts);
    }
    
    @Override
    public double getFailedLoginCount(String email) {
        AtomicInteger count = failedLoginCounts.get(email);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Sanitize email for use in metric tags.
     * Replaces @ and . to avoid issues with metric systems.
     * 
     * @param email Email address
     * @return Sanitized email
     */
    private String sanitizeEmail(String email) {
        if (email == null) {
            return "unknown";
        }
        return email.replace("@", "_at_").replace(".", "_");
    }
}

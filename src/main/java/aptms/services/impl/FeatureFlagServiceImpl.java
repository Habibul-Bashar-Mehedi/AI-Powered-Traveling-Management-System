package aptms.services.impl;

import aptms.services.FeatureFlagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of FeatureFlagService.
 * 
 * Reads feature flags from application configuration (environment variables).
 * 
 * Requirements: BR-5
 */
@Service
@Slf4j
public class FeatureFlagServiceImpl implements FeatureFlagService {
    
    @Value("${app.security.jwt.enabled:true}")
    private boolean jwtEnabled;
    
    /**
     * Check if JWT authentication is enabled.
     * 
     * Default value is false for backward compatibility.
     * Set JWT_ENABLED=true environment variable to enable JWT authentication.
     * 
     * @return true if JWT is enabled, false otherwise
     */
    @Override
    public boolean isJwtEnabled() {
        log.debug("JWT authentication enabled: {}", jwtEnabled);
        return jwtEnabled;
    }
}

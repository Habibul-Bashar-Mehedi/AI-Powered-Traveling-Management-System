package aptms.services;

/**
 * Service for managing feature flags.
 * 
 * Provides methods to check if specific features are enabled via configuration.
 * This allows for gradual rollout and easy rollback of new features.
 * 
 * Requirements: BR-5
 */
public interface FeatureFlagService {
    
    /**
     * Check if JWT authentication is enabled.
     * 
     * When false, the system falls back to session-based authentication.
     * When true, JWT authentication is active.
     * 
     * @return true if JWT is enabled, false otherwise
     */
    boolean isJwtEnabled();
}

package aptms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * JWT Configuration Properties
 * 
 * Binds JWT-related configuration from application.properties with prefix "app.security.jwt"
 * 
 * Configuration properties:
 * - secret: JWT signing secret (minimum 256 bits for HS256)
 * - accessTokenTtl: Access token time-to-live in milliseconds (default: 15 minutes)
 * - refreshTokenTtl: Refresh token time-to-live in milliseconds (default: 7 days)
 * - issuer: JWT issuer identifier (iss claim)
 * - audience: JWT audience identifier (aud claim)
 * - algorithm: JWT signing algorithm (HS256 or RS256)
 * 
 * @see SecurityProperties.Jwt
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.jwt")
@Validated
@Data
public class JwtConfigProperties {
    
    /**
     * JWT signing secret key
     * Must be at least 256 bits (32 characters) for HS256 algorithm
     * Should be stored in environment variable JWT_SECRET in production
     */
    @NotBlank(message = "JWT secret must not be blank")
    private String secret;
    
    /**
     * Access token time-to-live in milliseconds
     * Default: 900000ms (15 minutes)
     */
    @Min(value = 60000, message = "Access token TTL must be at least 60 seconds")
    private long accessTokenTtl = 900000; // 15 minutes
    
    /**
     * Refresh token time-to-live in milliseconds
     * Default: 604800000ms (7 days)
     */
    @Min(value = 3600000, message = "Refresh token TTL must be at least 1 hour")
    private long refreshTokenTtl = 604800000; // 7 days
    
    /**
     * JWT issuer identifier (iss claim)
     * Identifies the principal that issued the JWT
     */
    @NotBlank(message = "JWT issuer must not be blank")
    private String issuer = "com.aptms.auth";
    
    /**
     * JWT audience identifier (aud claim)
     * Identifies the recipients that the JWT is intended for
     */
    @NotBlank(message = "JWT audience must not be blank")
    private String audience = "com.aptms.api";
    
    /**
     * JWT signing algorithm
     * Supported values: HS256 (HMAC with SHA-256), RS256 (RSA with SHA-256)
     * Default: HS256
     */
    @Pattern(regexp = "HS256|RS256", message = "JWT algorithm must be HS256 or RS256")
    private String algorithm = "HS256";
    
    /**
     * Legacy expiration property (deprecated)
     * Use accessTokenTtl instead
     * @deprecated Use {@link #accessTokenTtl} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    private long expirationMs = 900000;
    
    /**
     * Get access token TTL in seconds
     * @return Access token TTL in seconds
     */
    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl / 1000;
    }
    
    /**
     * Get refresh token TTL in seconds
     * @return Refresh token TTL in seconds
     */
    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtl / 1000;
    }
    
    /**
     * Validate that the secret meets minimum length requirements
     * For HS256, secret must be at least 256 bits (32 characters)
     * @return true if secret is valid
     */
    public boolean isSecretValid() {
        if (secret == null) {
            return false;
        }
        
        if ("HS256".equals(algorithm)) {
            return secret.length() >= 32; // 256 bits minimum
        }
        
        return true; // RS256 uses key files, not secret string
    }
}

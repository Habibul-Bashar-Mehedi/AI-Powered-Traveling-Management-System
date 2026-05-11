package aptms.services;

import aptms.entities.RefreshToken;

import java.util.UUID;

/**
 * Service interface for managing refresh tokens and token blacklist.
 * 
 * Provides methods for:
 * - Storing and validating refresh tokens
 * - Revoking refresh tokens
 * - Managing token blacklist (Redis primary, MySQL fallback)
 * - Detecting refresh token reuse attacks
 * 
 * Requirements: FR-RFT-002, FR-RFT-003, FR-RFT-004, FR-LGT-001, 3.1.2
 */
public interface TokenService {
    
    /**
     * Store refresh token in database.
     * 
     * The token is stored as a BCrypt hash for security.
     * Device information, IP address, and user agent are captured for audit trail.
     * 
     * @param refreshToken RefreshToken entity with all required fields
     * @throws IllegalArgumentException if refreshToken is null or missing required fields
     */
    void storeRefreshToken(RefreshToken refreshToken);
    
    /**
     * Validate and retrieve refresh token.
     * 
     * Performs the following checks:
     * 1. Token hash exists in database
     * 2. Token is not expired
     * 3. Token is not revoked
     * 
     * @param token Token string (plain text, not hashed)
     * @return RefreshToken entity if valid
     * @throws aptms.exceptions.InvalidException if token is invalid, expired, or revoked
     */
    RefreshToken validateRefreshToken(String token);
    
    /**
     * Revoke specific refresh token.
     * 
     * Sets the revoked_at timestamp to mark the token as revoked.
     * Revoked tokens cannot be used to obtain new access tokens.
     * 
     * @param tokenId Token UUID
     * @throws aptms.exceptions.IdNotFoundException if token not found
     */
    void revokeRefreshToken(UUID tokenId);
    
    /**
     * Revoke all refresh tokens for user.
     * 
     * Used during:
     * - Logout from all devices
     * - Security events (password change, suspicious activity)
     * - Refresh token reuse detection
     * 
     * @param userId User UUID
     */
    void revokeAllUserTokens(UUID userId);
    
    /**
     * Check if access token is blacklisted.
     * 
     * Checks Redis cache first for fast lookup (< 5ms).
     * Falls back to MySQL if Redis is unavailable.
     * 
     * @param jti Token ID (jti claim from JWT)
     * @return true if token is blacklisted
     */
    boolean isTokenBlacklisted(String jti);
    
    /**
     * Add token to blacklist.
     * 
     * Adds the token's jti to both:
     * 1. Redis cache with TTL (auto-expires)
     * 2. MySQL database (persistent, for audit and Redis fallback)
     * 
     * @param jti Token ID (jti claim from JWT)
     * @param ttlSeconds Time to live in seconds (token's remaining lifetime)
     * @throws IllegalArgumentException if jti is null or blank, or ttlSeconds is negative
     */
    void addToBlacklist(String jti, long ttlSeconds);
    
    /**
     * Detect refresh token reuse.
     * 
     * Checks if a refresh token has already been used (revoked).
     * If reuse is detected, this is a security event indicating:
     * - Token theft
     * - Replay attack
     * - Compromised session
     * 
     * When reuse is detected, ALL user's refresh tokens should be revoked
     * using revokeAllUserTokens().
     * 
     * @param token Token string (plain text, not hashed)
     * @return true if token reuse is detected (token exists but is revoked)
     */
    boolean detectTokenReuse(String token);
}

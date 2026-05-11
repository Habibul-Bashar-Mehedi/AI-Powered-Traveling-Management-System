package aptms.services;

import aptms.entities.User;
import io.jsonwebtoken.Claims;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for JWT token operations.
 * 
 * Provides methods for generating, validating, and extracting information from JWT tokens.
 * Implements stateless authentication using JSON Web Tokens (RFC 7519).
 * 
 * Requirements: FR-LGN-004, FR-MID-002, 3.1.1
 */
public interface JwtService {
    
    /**
     * Generate access token with 15-minute TTL.
     * 
     * Creates a signed JWT token containing user information and claims:
     * - sub: User UUID
     * - iat: Issued at timestamp
     * - exp: Expiration timestamp (15 minutes from now)
     * - jti: Unique token ID (UUID v4)
     * - iss: Issuer identifier
     * - aud: Audience identifier
     * - roles: Array of user roles
     * - email: User email
     * 
     * @param user User entity
     * @return Signed JWT token string
     * @throws IllegalArgumentException if user is null or missing required fields
     */
    String generateAccessToken(User user);
    
    /**
     * Generate refresh token with 7-day TTL.
     * 
     * Creates a secure random token string used to obtain new access tokens.
     * The refresh token is stored as a BCrypt hash in the database.
     * 
     * @param user User entity
     * @return Secure random token string (not a JWT)
     * @throws IllegalArgumentException if user is null
     */
    String generateRefreshToken(User user);
    
    /**
     * Validate token signature and expiration.
     * 
     * Performs the following checks:
     * 1. Token structure is valid JWT
     * 2. Signature is valid using configured secret
     * 3. Token is not expired (with 30-second clock skew tolerance)
     * 4. Issuer (iss) matches expected value
     * 5. Audience (aud) matches expected value
     * 
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);
    
    /**
     * Extract all claims from token.
     * 
     * Parses the JWT token and returns the claims payload.
     * Does not validate the token - use validateToken() first.
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     * @throws io.jsonwebtoken.JwtException if token is malformed or invalid
     */
    Claims extractClaims(String token);
    
    /**
     * Extract user ID from token.
     * 
     * Retrieves the user UUID from the 'sub' (subject) claim.
     * 
     * @param token JWT token string
     * @return User UUID
     * @throws io.jsonwebtoken.JwtException if token is malformed or invalid
     * @throws IllegalArgumentException if sub claim is not a valid UUID
     */
    UUID extractUserId(String token);
    
    /**
     * Extract roles from token.
     * 
     * Retrieves the list of role strings from the 'roles' claim.
     * 
     * @param token JWT token string
     * @return List of role strings (e.g., ["USER", "ADMIN"])
     * @throws io.jsonwebtoken.JwtException if token is malformed or invalid
     */
    List<String> extractRoles(String token);
    
    /**
     * Extract email from token.
     * 
     * Retrieves the user email from the 'email' claim.
     * 
     * @param token JWT token string
     * @return User email address
     * @throws io.jsonwebtoken.JwtException if token is malformed or invalid
     */
    String extractEmail(String token);
    
    /**
     * Check if token is expired.
     * 
     * Compares the 'exp' (expiration) claim against the current time
     * with a 30-second clock skew tolerance.
     * 
     * @param token JWT token string
     * @return true if token is expired
     * @throws io.jsonwebtoken.JwtException if token is malformed or invalid
     */
    boolean isTokenExpired(String token);
}

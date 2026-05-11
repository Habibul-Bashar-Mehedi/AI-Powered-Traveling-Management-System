package aptms.services.impl;

import aptms.config.properties.JwtConfigProperties;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of JWT token operations using JJWT library.
 * 
 * Uses HS256 algorithm (HMAC with SHA-256) for token signing.
 * Implements RFC 7519 JWT standard with required claims.
 * 
 * Requirements: FR-LGN-004, FR-MID-002, 3.1.1
 */
@Service
public class JwtServiceImpl implements JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);
    private static final int REFRESH_TOKEN_LENGTH = 64; // 64 bytes = 512 bits
    private static final long CLOCK_SKEW_SECONDS = 30; // 30-second clock skew tolerance
    
    private final JwtConfigProperties jwtConfig;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    
    public JwtServiceImpl(JwtConfigProperties jwtConfig) {
        this.jwtConfig = jwtConfig;
        
        // Validate secret key length for HS256
        if (!jwtConfig.isSecretValid()) {
            throw new IllegalStateException(
                "JWT secret must be at least 256 bits (32 characters) for HS256 algorithm"
            );
        }
        
        // Create secret key from configuration
        this.secretKey = Keys.hmacShaKeyFor(
            jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
        );
        
        this.secureRandom = new SecureRandom();
        
        logger.info("JwtService initialized with algorithm: {}, access token TTL: {}s, refresh token TTL: {}s",
            jwtConfig.getAlgorithm(),
            jwtConfig.getAccessTokenTtlSeconds(),
            jwtConfig.getRefreshTokenTtlSeconds()
        );
    }
    
    @Override
    public String generateAccessToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or blank");
        }
        if (user.getRole() == null) {
            throw new IllegalArgumentException("User role cannot be null");
        }
        
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtConfig.getAccessTokenTtl());
        
        String token = Jwts.builder()
            .subject(user.getId().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .id(UUID.randomUUID().toString())
            .issuer(jwtConfig.getIssuer())
            .audience().add(jwtConfig.getAudience()).and()
            .claim("roles", List.of(user.getRole().name()))
            .claim("email", user.getEmail())
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
        
        logger.debug("Generated access token for user: {} (expires: {})", user.getId(), expiration);
        return token;
    }
    
    @Override
    public String generateRefreshToken(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        // Generate cryptographically secure random token
        byte[] randomBytes = new byte[REFRESH_TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        logger.debug("Generated refresh token for user: {}", user.getId());
        return token;
    }
    
    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .requireIssuer(jwtConfig.getIssuer())
                .requireAudience(jwtConfig.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            // Additional validation: check if token is expired
            if (claims.getExpiration().before(new Date())) {
                logger.debug("Token is expired: {}", claims.getId());
                return false;
            }
            
            logger.debug("Token validated successfully: {}", claims.getId());
            return true;
            
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public Claims extractClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }
        
        return Jwts.parser()
            .verifyWith(secretKey)
            .clockSkewSeconds(CLOCK_SKEW_SECONDS)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    @Override
    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        String subject = claims.getSubject();
        
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Token subject (user ID) is missing");
        }
        
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token subject is not a valid UUID: " + subject, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        Object rolesObj = claims.get("roles");
        
        if (rolesObj == null) {
            throw new IllegalArgumentException("Token does not contain roles claim");
        }
        
        if (!(rolesObj instanceof List)) {
            throw new IllegalArgumentException("Roles claim is not a list");
        }
        
        return (List<String>) rolesObj;
    }
    
    @Override
    public String extractEmail(String token) {
        Claims claims = extractClaims(token);
        String email = claims.get("email", String.class);
        
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token does not contain email claim");
        }
        
        return email;
    }
    
    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            
            // Check if expired with clock skew tolerance
            Instant expirationInstant = expiration.toInstant();
            Instant now = Instant.now();
            Instant expirationWithSkew = expirationInstant.plusSeconds(CLOCK_SKEW_SECONDS);
            
            boolean expired = now.isAfter(expirationWithSkew);
            
            if (expired) {
                logger.debug("Token {} is expired (exp: {}, now: {})", 
                    claims.getId(), expirationInstant, now);
            }
            
            return expired;
            
        } catch (Exception e) {
            logger.debug("Error checking token expiration: {}", e.getMessage());
            return true; // Treat invalid tokens as expired
        }
    }
}

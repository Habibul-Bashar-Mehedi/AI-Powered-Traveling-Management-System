package aptms.services.impl;

import aptms.config.properties.JwtConfigProperties;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.services.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtServiceImpl.
 * 
 * Tests core JWT functionality including token generation, validation,
 * and claim extraction.
 * 
 * Requirements: FR-LGN-004, FR-MID-002, 3.1.1
 */
class JwtServiceImplTest {
    
    private JwtService jwtService;
    private JwtConfigProperties jwtConfig;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // Create test configuration
        jwtConfig = new JwtConfigProperties();
        jwtConfig.setSecret("mySecretKeyForJWTTokenGenerationPleaseChangeInProduction256BitMinimum");
        jwtConfig.setAccessTokenTtl(900000); // 15 minutes
        jwtConfig.setRefreshTokenTtl(604800000); // 7 days
        jwtConfig.setIssuer("com.aptms.auth");
        jwtConfig.setAudience("com.aptms.api");
        jwtConfig.setAlgorithm("HS256");
        
        // Create service
        jwtService = new JwtServiceImpl(jwtConfig);
        
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
    }
    
    @Test
    void testGenerateAccessToken_Success() {
        // When
        String token = jwtService.generateAccessToken(testUser);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts: header.payload.signature
    }
    
    @Test
    void testGenerateAccessToken_NullUser_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(null);
        });
    }
    
    @Test
    void testGenerateAccessToken_NullUserId_ThrowsException() {
        // Given
        testUser.setId(null);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(testUser);
        });
    }
    
    @Test
    void testGenerateAccessToken_NullEmail_ThrowsException() {
        // Given
        testUser.setEmail(null);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(testUser);
        });
    }
    
    @Test
    void testGenerateAccessToken_NullRole_ThrowsException() {
        // Given
        testUser.setRole(null);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateAccessToken(testUser);
        });
    }
    
    @Test
    void testGenerateRefreshToken_Success() {
        // When
        String token = jwtService.generateRefreshToken(testUser);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.length() > 50); // Should be a long random string
    }
    
    @Test
    void testGenerateRefreshToken_NullUser_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            jwtService.generateRefreshToken(null);
        });
    }
    
    @Test
    void testGenerateRefreshToken_UniqueTokens() {
        // When
        String token1 = jwtService.generateRefreshToken(testUser);
        String token2 = jwtService.generateRefreshToken(testUser);
        
        // Then
        assertNotEquals(token1, token2, "Refresh tokens should be unique");
    }
    
    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        boolean isValid = jwtService.validateToken(token);
        
        // Then
        assertTrue(isValid);
    }
    
    @Test
    void testValidateToken_NullToken_ReturnsFalse() {
        // When
        boolean isValid = jwtService.validateToken(null);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_EmptyToken_ReturnsFalse() {
        // When
        boolean isValid = jwtService.validateToken("");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        // When
        boolean isValid = jwtService.validateToken("not.a.valid.jwt");
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testValidateToken_TamperedToken_ReturnsFalse() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";
        
        // When
        boolean isValid = jwtService.validateToken(tamperedToken);
        
        // Then
        assertFalse(isValid);
    }
    
    @Test
    void testExtractClaims_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        Claims claims = jwtService.extractClaims(token);
        
        // Then
        assertNotNull(claims);
        assertNotNull(claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());
        assertEquals(jwtConfig.getIssuer(), claims.getIssuer());
        assertTrue(claims.getAudience().contains(jwtConfig.getAudience()));
    }
    
    @Test
    void testExtractUserId_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        UUID userId = jwtService.extractUserId(token);
        
        // Then
        assertEquals(testUser.getId(), userId);
    }
    
    @Test
    void testExtractRoles_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        List<String> roles = jwtService.extractRoles(token);
        
        // Then
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals(UserRole.USER.name(), roles.get(0));
    }
    
    @Test
    void testExtractRoles_AdminUser() {
        // Given
        testUser.setRole(UserRole.ADMIN);
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        List<String> roles = jwtService.extractRoles(token);
        
        // Then
        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals(UserRole.ADMIN.name(), roles.get(0));
    }
    
    @Test
    void testExtractEmail_Success() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        String email = jwtService.extractEmail(token);
        
        // Then
        assertEquals(testUser.getEmail(), email);
    }
    
    @Test
    void testIsTokenExpired_ValidToken_ReturnsFalse() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        boolean isExpired = jwtService.isTokenExpired(token);
        
        // Then
        assertFalse(isExpired);
    }
    
    @Test
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() throws InterruptedException {
        // Given - create config with very short TTL
        JwtConfigProperties shortTtlConfig = new JwtConfigProperties();
        shortTtlConfig.setSecret("mySecretKeyForJWTTokenGenerationPleaseChangeInProduction256BitMinimum");
        shortTtlConfig.setAccessTokenTtl(1000); // 1 second
        shortTtlConfig.setIssuer("com.aptms.auth");
        shortTtlConfig.setAudience("com.aptms.api");
        shortTtlConfig.setAlgorithm("HS256");
        
        JwtService shortTtlService = new JwtServiceImpl(shortTtlConfig);
        String token = shortTtlService.generateAccessToken(testUser);
        
        // Wait for token to expire (1 second + 30 second clock skew + buffer)
        Thread.sleep(32000);
        
        // When
        boolean isExpired = shortTtlService.isTokenExpired(token);
        
        // Then
        assertTrue(isExpired);
    }
    
    @Test
    void testTokenContainsAllRequiredClaims() {
        // Given
        String token = jwtService.generateAccessToken(testUser);
        
        // When
        Claims claims = jwtService.extractClaims(token);
        
        // Then - verify all required claims are present
        assertNotNull(claims.getSubject(), "sub claim should be present");
        assertNotNull(claims.getIssuedAt(), "iat claim should be present");
        assertNotNull(claims.getExpiration(), "exp claim should be present");
        assertNotNull(claims.getId(), "jti claim should be present");
        assertNotNull(claims.getIssuer(), "iss claim should be present");
        assertNotNull(claims.getAudience(), "aud claim should be present");
        assertNotNull(claims.get("roles"), "roles claim should be present");
        assertNotNull(claims.get("email"), "email claim should be present");
    }
    
    @Test
    void testTokenExpirationTime() {
        // Given
        Instant beforeGeneration = Instant.now();
        String token = jwtService.generateAccessToken(testUser);
        Instant afterGeneration = Instant.now();
        
        // When
        Claims claims = jwtService.extractClaims(token);
        Instant expiration = claims.getExpiration().toInstant();
        Instant issuedAt = claims.getIssuedAt().toInstant();
        
        // Then
        // Expiration should be approximately 15 minutes (900 seconds) after issuedAt
        long ttlSeconds = expiration.getEpochSecond() - issuedAt.getEpochSecond();
        assertEquals(jwtConfig.getAccessTokenTtlSeconds(), ttlSeconds, 1); // Allow 1 second tolerance
        
        // IssuedAt should be between before and after generation
        assertTrue(issuedAt.isAfter(beforeGeneration.minusSeconds(1)));
        assertTrue(issuedAt.isBefore(afterGeneration.plusSeconds(1)));
    }
}

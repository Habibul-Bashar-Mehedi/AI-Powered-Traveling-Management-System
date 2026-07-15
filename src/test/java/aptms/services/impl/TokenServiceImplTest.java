package aptms.services.impl;

import aptms.entities.RefreshToken;
import aptms.entities.User;
import aptms.enums.BlacklistReason;
import aptms.enums.UserRole;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RefreshTokenRepository;
import aptms.repositories.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenServiceImpl.
 * 
 * Tests core token management functionality:
 * - Refresh token storage and validation
 * - Token revocation
 * - Blacklist operations
 * - Token reuse detection
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    private TokenServiceImpl tokenService;
    
    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(
            refreshTokenRepository,
            tokenBlacklistRepository,
            redisTemplate
        );
    }
    
    @Test
    void testStoreRefreshToken_Success() {
        // Arrange
        User user = createTestUser();
        RefreshToken token = createTestRefreshToken(user, "test-token");
        
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);
        
        // Act
        tokenService.storeRefreshToken(token);
        
        // Assert
        verify(refreshTokenRepository, times(1)).save(token);
    }
    
    @Test
    void testStoreRefreshToken_NullToken_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.storeRefreshToken(null);
        });
    }
    
    @Test
    void testStoreRefreshToken_NullUser_ThrowsException() {
        // Arrange
        RefreshToken token = new RefreshToken();
        token.setTokenHash("hash");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            tokenService.storeRefreshToken(token);
        });
    }
    
    @Test
    void testValidateRefreshToken_Success() {
        // Arrange
        User user = createTestUser();
        String plainToken = "test-token-12345";
        String sha256Hash = sha256(plainToken);
        
        RefreshToken token = createTestRefreshToken(user, sha256Hash);
        token.setRevokedAt(null);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(refreshTokenRepository.findByTokenHash(sha256Hash)).thenReturn(Optional.of(token));
        
        // Act
        RefreshToken result = tokenService.validateRefreshToken(plainToken);
        
        // Assert
        assertNotNull(result);
        assertEquals(token.getId(), result.getId());
        assertEquals(user.getId(), result.getUser().getId());
    }
    
    @Test
    void testValidateRefreshToken_ExpiredToken_ThrowsException() {
        // Arrange
        User user = createTestUser();
        String plainToken = "test-token-12345";
        String sha256Hash = sha256(plainToken);
        
        RefreshToken token = createTestRefreshToken(user, sha256Hash);
        token.setRevokedAt(null);
        token.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired
        
        when(refreshTokenRepository.findByTokenHash(sha256Hash)).thenReturn(Optional.of(token));
        
        // Act & Assert
        assertThrows(InvalidException.class, () -> {
            tokenService.validateRefreshToken(plainToken);
        });
    }
    
    @Test
    void testValidateRefreshToken_RevokedToken_ThrowsException() {
        // Arrange
        User user = createTestUser();
        String plainToken = "test-token-12345";
        String sha256Hash = sha256(plainToken);
        
        RefreshToken token = createTestRefreshToken(user, sha256Hash);
        token.setRevokedAt(Instant.now()); // Revoked
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(refreshTokenRepository.findByTokenHash(sha256Hash)).thenReturn(Optional.of(token));
        
        // Act & Assert
        assertThrows(InvalidException.class, () -> {
            tokenService.validateRefreshToken(plainToken);
        });
    }
    
    @Test
    void testRevokeRefreshToken_Success() {
        // Arrange
        User user = createTestUser();
        RefreshToken token = createTestRefreshToken(user, "hash");
        UUID tokenId = token.getId();
        
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(token);
        
        // Act
        tokenService.revokeRefreshToken(tokenId);
        
        // Assert
        verify(refreshTokenRepository, times(1)).findById(tokenId);
        verify(refreshTokenRepository, times(1)).save(token);
        assertNotNull(token.getRevokedAt());
    }
    
    @Test
    void testRevokeRefreshToken_TokenNotFound_ThrowsException() {
        // Arrange
        UUID tokenId = UUID.randomUUID();
        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IdNotFoundException.class, () -> {
            tokenService.revokeRefreshToken(tokenId);
        });
    }
    
    @Test
    void testRevokeAllUserTokens_Success() {
        // Arrange
        User user = createTestUser();
        UUID userId = user.getId();
        
        RefreshToken token1 = createTestRefreshToken(user, "hash1");
        RefreshToken token2 = createTestRefreshToken(user, "hash2");
        List<RefreshToken> tokens = List.of(token1, token2);
        
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId)).thenReturn(tokens);
        when(refreshTokenRepository.saveAll(anyList())).thenReturn(tokens);
        
        // Act
        tokenService.revokeAllUserTokens(userId);
        
        // Assert
        verify(refreshTokenRepository, times(1)).findByUserIdAndRevokedAtIsNull(userId);
        verify(refreshTokenRepository, times(1)).saveAll(anyList());
        assertNotNull(token1.getRevokedAt());
        assertNotNull(token2.getRevokedAt());
    }
    
    @Test
    void testIsTokenBlacklisted_InRedis_ReturnsTrue() {
        // Arrange
        String jti = "test-jti-123";
        String redisKey = "blacklist:" + jti;
        
        when(redisTemplate.hasKey(redisKey)).thenReturn(true);
        
        // Act
        boolean result = tokenService.isTokenBlacklisted(jti);
        
        // Assert
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey(redisKey);
        verify(tokenBlacklistRepository, never()).findByJti(anyString());
    }
    
    @Test
    void testIsTokenBlacklisted_NotInRedisButInMySQL_ReturnsTrue() {
        // Arrange
        String jti = "test-jti-123";
        String redisKey = "blacklist:" + jti;
        
        User user = createTestUser();
        aptms.entities.TokenBlacklist blacklistEntry = new aptms.entities.TokenBlacklist();
        blacklistEntry.setJti(jti);
        blacklistEntry.setUser(user);
        blacklistEntry.setReason(BlacklistReason.LOGOUT);
        blacklistEntry.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(redisTemplate.hasKey(redisKey)).thenReturn(false);
        when(tokenBlacklistRepository.findByJti(jti)).thenReturn(Optional.of(blacklistEntry));
        
        // Act
        boolean result = tokenService.isTokenBlacklisted(jti);
        
        // Assert
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey(redisKey);
        verify(tokenBlacklistRepository, times(1)).findByJti(jti);
    }
    
    @Test
    void testIsTokenBlacklisted_NotFound_ReturnsFalse() {
        // Arrange
        String jti = "test-jti-123";
        String redisKey = "blacklist:" + jti;
        
        when(redisTemplate.hasKey(redisKey)).thenReturn(false);
        when(tokenBlacklistRepository.findByJti(jti)).thenReturn(Optional.empty());
        
        // Act
        boolean result = tokenService.isTokenBlacklisted(jti);
        
        // Assert
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(redisKey);
        verify(tokenBlacklistRepository, times(1)).findByJti(jti);
    }
    
    @Test
    void testDetectTokenReuse_RevokedToken_ReturnsTrue() {
        // Arrange
        User user = createTestUser();
        String plainToken = "test-token-12345";
        String sha256Hash = sha256(plainToken);
        
        RefreshToken token = createTestRefreshToken(user, sha256Hash);
        token.setRevokedAt(Instant.now()); // Revoked
        
        when(refreshTokenRepository.findByTokenHash(sha256Hash)).thenReturn(Optional.of(token));
        
        // Act
        boolean result = tokenService.detectTokenReuse(plainToken);
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    void testDetectTokenReuse_ValidToken_ReturnsFalse() {
        // Arrange
        User user = createTestUser();
        String plainToken = "test-token-12345";
        String sha256Hash = sha256(plainToken);
        
        RefreshToken token = createTestRefreshToken(user, sha256Hash);
        token.setRevokedAt(null); // Not revoked
        
        when(refreshTokenRepository.findByTokenHash(sha256Hash)).thenReturn(Optional.of(token));
        
        // Act
        boolean result = tokenService.detectTokenReuse(plainToken);
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    void testDetectTokenReuse_TokenNotFound_ReturnsFalse() {
        // Arrange
        String plainToken = "test-token-12345";
        
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        
        // Act
        boolean result = tokenService.detectTokenReuse(plainToken);
        
        // Assert
        assertFalse(result);
    }
    
    // Helper methods
    
    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        user.setRole(UserRole.USER);
        return user;
    }
    
    private RefreshToken createTestRefreshToken(User user, String tokenHash) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash(tokenHash);
        token.setDeviceInfo("Test Device");
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test Agent");
        token.setExpiresAt(Instant.now().plusSeconds(604800)); // 7 days
        token.setCreatedAt(Instant.now());
        token.setUpdatedAt(Instant.now());
        return token;
    }
    
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

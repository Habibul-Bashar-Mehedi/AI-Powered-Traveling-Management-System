package aptms.services;

import aptms.entities.RefreshToken;
import aptms.entities.TokenBlacklist;
import aptms.entities.User;
import aptms.enums.BlacklistReason;
import aptms.enums.UserRole;
import aptms.repositories.RefreshTokenRepository;
import aptms.repositories.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenCleanupService.
 * 
 * Tests scheduled cleanup jobs for:
 * - Expired blacklist entries
 * - Expired and revoked refresh tokens
 */
@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {
    
    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    private TokenCleanupService tokenCleanupService;
    
    @BeforeEach
    void setUp() {
        tokenCleanupService = new TokenCleanupService(
            tokenBlacklistRepository,
            refreshTokenRepository
        );
    }
    
    @Test
    void testCleanupExpiredBlacklistEntries_Success() {
        // Arrange
        int expectedDeletedCount = 5;
        when(tokenBlacklistRepository.deleteByExpiresAtBefore(any(Instant.class)))
            .thenReturn(expectedDeletedCount);
        
        // Act
        tokenCleanupService.cleanupExpiredBlacklistEntries();
        
        // Assert
        verify(tokenBlacklistRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
    }
    
    @Test
    void testCleanupExpiredBlacklistEntries_NoEntriesToDelete() {
        // Arrange
        when(tokenBlacklistRepository.deleteByExpiresAtBefore(any(Instant.class)))
            .thenReturn(0);
        
        // Act
        tokenCleanupService.cleanupExpiredBlacklistEntries();
        
        // Assert
        verify(tokenBlacklistRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
    }
    
    @Test
    void testCleanupExpiredBlacklistEntries_ExceptionHandled() {
        // Arrange
        when(tokenBlacklistRepository.deleteByExpiresAtBefore(any(Instant.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act - should not throw exception
        tokenCleanupService.cleanupExpiredBlacklistEntries();
        
        // Assert
        verify(tokenBlacklistRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
    }
    
    @Test
    void testCleanupExpiredRefreshTokens_Success() {
        // Arrange
        int expectedDeletedCount = 10;
        when(refreshTokenRepository.deleteExpiredOrRevokedTokens(any(Instant.class)))
            .thenReturn(expectedDeletedCount);
        
        // Act
        tokenCleanupService.cleanupExpiredRefreshTokens();
        
        // Assert
        verify(refreshTokenRepository, times(1)).deleteExpiredOrRevokedTokens(any(Instant.class));
    }
    
    @Test
    void testCleanupExpiredRefreshTokens_NoEntriesToDelete() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredOrRevokedTokens(any(Instant.class)))
            .thenReturn(0);
        
        // Act
        tokenCleanupService.cleanupExpiredRefreshTokens();
        
        // Assert
        verify(refreshTokenRepository, times(1)).deleteExpiredOrRevokedTokens(any(Instant.class));
    }
    
    @Test
    void testCleanupExpiredRefreshTokens_ExceptionHandled() {
        // Arrange
        when(refreshTokenRepository.deleteExpiredOrRevokedTokens(any(Instant.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act - should not throw exception
        tokenCleanupService.cleanupExpiredRefreshTokens();
        
        // Assert
        verify(refreshTokenRepository, times(1)).deleteExpiredOrRevokedTokens(any(Instant.class));
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
    
    private TokenBlacklist createTestBlacklistEntry(User user, String jti, Instant expiresAt) {
        TokenBlacklist entry = new TokenBlacklist();
        entry.setJti(jti);
        entry.setUser(user);
        entry.setReason(BlacklistReason.LOGOUT);
        entry.setExpiresAt(expiresAt);
        entry.setCreatedAt(Instant.now());
        return entry;
    }
    
    private RefreshToken createTestRefreshToken(User user, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash("hashed-token");
        token.setDeviceInfo("Test Device");
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test Agent");
        token.setExpiresAt(expiresAt);
        token.setRevokedAt(revokedAt);
        token.setCreatedAt(Instant.now());
        token.setUpdatedAt(Instant.now());
        return token;
    }
}

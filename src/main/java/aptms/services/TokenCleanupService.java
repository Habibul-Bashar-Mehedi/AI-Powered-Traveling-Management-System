package aptms.services;

import aptms.repositories.RefreshTokenRepository;
import aptms.repositories.TokenBlacklistRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for scheduled cleanup of expired tokens.
 * 
 * This service runs periodic cleanup jobs to remove:
 * - Expired blacklist entries (hourly)
 * - Expired and revoked refresh tokens (daily)
 * 
 * Requirements: FR-LGT-004, FR-RFT-002
 */
@Service
public class TokenCleanupService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupService.class);
    
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    
    public TokenCleanupService(
            TokenBlacklistRepository tokenBlacklistRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    /**
     * Scheduled job to clean up expired blacklist entries.
     * Runs every hour at the top of the hour.
     * 
     * Deletes TokenBlacklist entries where expires_at < NOW()
     * 
     * Requirements: FR-LGT-004
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @Transactional
    public void cleanupExpiredBlacklistEntries() {
        logger.info("Starting blacklist cleanup job");
        
        try {
            Instant now = Instant.now();
            int deletedCount = tokenBlacklistRepository.deleteByExpiresAtBefore(now);
            
            logger.info("Blacklist cleanup completed: {} entries deleted", deletedCount);
        } catch (Exception e) {
            logger.error("Error during blacklist cleanup", e);
        }
    }
    
    /**
     * Scheduled job to clean up expired and revoked refresh tokens.
     * Runs daily at 2:00 AM.
     * 
     * Deletes RefreshToken entries where:
     * - expires_at < NOW() OR
     * - revoked_at IS NOT NULL
     * 
     * Requirements: FR-RFT-002
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2:00 AM
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        logger.info("Starting refresh token cleanup job");
        
        try {
            Instant now = Instant.now();
            int deletedCount = refreshTokenRepository.deleteExpiredOrRevokedTokens(now);
            
            logger.info("Refresh token cleanup completed: {} entries deleted", deletedCount);
        } catch (Exception e) {
            logger.error("Error during refresh token cleanup", e);
        }
    }
}

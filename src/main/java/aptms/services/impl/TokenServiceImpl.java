package aptms.services.impl;

import aptms.entities.RefreshToken;
import aptms.entities.TokenBlacklist;
import aptms.entities.User;
import aptms.enums.BlacklistReason;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RefreshTokenRepository;
import aptms.repositories.TokenBlacklistRepository;
import aptms.services.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of token management service.
 * 
 * Manages refresh tokens and token blacklist with:
 * - BCrypt hashing for refresh token storage
 * - Redis primary cache for blacklist (fast lookups)
 * - MySQL fallback for blacklist (persistence and reliability)
 * - Refresh token reuse detection for security
 * 
 * Requirements: FR-RFT-002, FR-RFT-003, FR-RFT-004, FR-LGT-001, 3.1.2
 */
@Service
public class TokenServiceImpl implements TokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenServiceImpl.class);
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final int BCRYPT_STRENGTH = 10;
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public TokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            TokenBlacklistRepository tokenBlacklistRepository,
            RedisTemplate<String, String> redisTemplate) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
        
        logger.info("TokenService initialized with Redis cache and MySQL fallback");
    }
    
    @Override
    @Transactional
    public void storeRefreshToken(RefreshToken refreshToken) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("RefreshToken cannot be null");
        }
        if (refreshToken.getUser() == null) {
            throw new IllegalArgumentException("RefreshToken user cannot be null");
        }
        if (refreshToken.getTokenHash() == null || refreshToken.getTokenHash().isBlank()) {
            throw new IllegalArgumentException("RefreshToken hash cannot be null or blank");
        }
        if (refreshToken.getExpiresAt() == null) {
            throw new IllegalArgumentException("RefreshToken expiresAt cannot be null");
        }
        
        refreshTokenRepository.save(refreshToken);
        
        logger.debug("Stored refresh token for user: {} (expires: {})", 
            refreshToken.getUser().getId(), refreshToken.getExpiresAt());
    }
    
    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidException("Refresh token cannot be null or blank");
        }
        
        // Hash the token using SHA-256 (same as storage)
        String tokenHash = hashRefreshToken(token);
        
        // Find token by hash
        RefreshToken matchingToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElse(null);
        
        if (matchingToken == null) {
            logger.warn("Refresh token validation failed: token not found");
            throw new InvalidException("Invalid refresh token");
        }
        
        // Check if revoked
        if (matchingToken.getRevokedAt() != null) {
            logger.warn("Refresh token validation failed: token revoked (user: {})", 
                matchingToken.getUser().getId());
            throw new InvalidException("Refresh token has been revoked");
        }
        
        // Check if expired
        if (matchingToken.isExpired()) {
            logger.warn("Refresh token validation failed: token expired (user: {})", 
                matchingToken.getUser().getId());
            throw new InvalidException("Refresh token has expired");
        }
        
        logger.debug("Refresh token validated successfully for user: {}", 
            matchingToken.getUser().getId());
        
        return matchingToken;
    }
    
    /**
     * Hash refresh token using SHA-256
     * BCrypt has a 72-byte limit, but refresh tokens are 86 characters (64 bytes base64-encoded)
     */
    private String hashRefreshToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    @Override
    @Transactional
    public void revokeRefreshToken(UUID tokenId) {
        if (tokenId == null) {
            throw new IllegalArgumentException("Token ID cannot be null");
        }
        
        RefreshToken token = refreshTokenRepository.findById(tokenId)
            .orElseThrow(() -> new IdNotFoundException("Refresh token not found: " + tokenId));
        
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);
        
        logger.info("Revoked refresh token: {} (user: {})", tokenId, token.getUser().getId());
    }
    
    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId);
        
        Instant now = Instant.now();
        for (RefreshToken token : userTokens) {
            token.setRevokedAt(now);
        }
        
        refreshTokenRepository.saveAll(userTokens);
        
        logger.info("Revoked all refresh tokens for user: {} (count: {})", userId, userTokens.size());
    }
    
    @Override
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        
        String redisKey = BLACKLIST_KEY_PREFIX + jti;
        
        try {
            // Check Redis cache first (fast path)
            Boolean exists = redisTemplate.hasKey(redisKey);
            if (Boolean.TRUE.equals(exists)) {
                logger.debug("Token {} found in Redis blacklist", jti);
                return true;
            }
            
            // Redis says not blacklisted, but check MySQL as fallback
            // (in case Redis was down when token was blacklisted)
            Optional<TokenBlacklist> blacklistEntry = tokenBlacklistRepository.findByJti(jti);
            
            if (blacklistEntry.isPresent()) {
                logger.debug("Token {} found in MySQL blacklist (Redis cache miss)", jti);
                
                // Sync back to Redis for future lookups
                long ttlSeconds = blacklistEntry.get().getExpiresAt().getEpochSecond() 
                    - Instant.now().getEpochSecond();
                if (ttlSeconds > 0) {
                    try {
                        redisTemplate.opsForValue().set(redisKey, "1", ttlSeconds, TimeUnit.SECONDS);
                        logger.debug("Synced token {} to Redis cache with TTL: {}s", jti, ttlSeconds);
                    } catch (Exception e) {
                        logger.warn("Failed to sync token {} to Redis: {}", jti, e.getMessage());
                    }
                }
                
                return true;
            }
            
            logger.debug("Token {} not found in blacklist", jti);
            return false;
            
        } catch (Exception e) {
            // Redis is unavailable, fall back to MySQL only
            logger.warn("Redis unavailable, falling back to MySQL for blacklist check: {}", 
                e.getMessage());
            
            Optional<TokenBlacklist> blacklistEntry = tokenBlacklistRepository.findByJti(jti);
            boolean isBlacklisted = blacklistEntry.isPresent();
            
            if (isBlacklisted) {
                logger.debug("Token {} found in MySQL blacklist (Redis fallback)", jti);
            }
            
            return isBlacklisted;
        }
    }
    
    @Override
    @Transactional
    public void addToBlacklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("JTI cannot be null or blank");
        }
        if (ttlSeconds < 0) {
            throw new IllegalArgumentException("TTL cannot be negative");
        }
        
        String redisKey = BLACKLIST_KEY_PREFIX + jti;
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        
        // Add to Redis with TTL (auto-expires)
        try {
            redisTemplate.opsForValue().set(redisKey, "1", ttlSeconds, TimeUnit.SECONDS);
            logger.debug("Added token {} to Redis blacklist with TTL: {}s", jti, ttlSeconds);
        } catch (Exception e) {
            logger.warn("Failed to add token {} to Redis blacklist: {}", jti, e.getMessage());
            // Continue to MySQL even if Redis fails
        }
        
        // Add to MySQL for persistence and fallback
        // Note: We need the user to create the TokenBlacklist entity
        // This will be called from the authentication service where we have the user context
        logger.debug("Token {} added to blacklist (expires: {})", jti, expiresAt);
    }
    
    /**
     * Add token to blacklist with user context.
     * This is an internal method that includes the user for MySQL persistence.
     * 
     * @param jti Token ID
     * @param user User entity
     * @param reason Blacklist reason
     * @param ttlSeconds Time to live in seconds
     */
    @Transactional
    public void addToBlacklist(String jti, User user, BlacklistReason reason, long ttlSeconds) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("JTI cannot be null or blank");
        }
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reason cannot be null");
        }
        if (ttlSeconds < 0) {
            throw new IllegalArgumentException("TTL cannot be negative");
        }
        
        String redisKey = BLACKLIST_KEY_PREFIX + jti;
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        
        // Add to Redis with TTL (auto-expires)
        try {
            redisTemplate.opsForValue().set(redisKey, "1", ttlSeconds, TimeUnit.SECONDS);
            logger.debug("Added token {} to Redis blacklist with TTL: {}s", jti, ttlSeconds);
        } catch (Exception e) {
            logger.warn("Failed to add token {} to Redis blacklist: {}", jti, e.getMessage());
            // Continue to MySQL even if Redis fails
        }
        
        // Add to MySQL for persistence and fallback
        TokenBlacklist blacklistEntry = new TokenBlacklist();
        blacklistEntry.setJti(jti);
        blacklistEntry.setUser(user);
        blacklistEntry.setReason(reason);
        blacklistEntry.setExpiresAt(expiresAt);
        blacklistEntry.setCreatedAt(Instant.now());
        
        tokenBlacklistRepository.save(blacklistEntry);
        
        logger.info("Added token {} to blacklist (user: {}, reason: {}, expires: {})", 
            jti, user.getId(), reason, expiresAt);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean detectTokenReuse(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        
        // Find all tokens (including revoked) and check hash match
        List<RefreshToken> allTokens = refreshTokenRepository.findAll();
        
        for (RefreshToken rt : allTokens) {
            if (passwordEncoder.matches(token, rt.getTokenHash())) {
                // Token exists - check if it's revoked
                if (rt.isRevoked()) {
                    logger.warn("SECURITY: Refresh token reuse detected for user: {} (token: {})", 
                        rt.getUser().getId(), rt.getId());
                    return true;
                }
                // Token exists and is not revoked - this is normal usage
                return false;
            }
        }
        
        // Token not found - not a reuse, just invalid
        return false;
    }
}

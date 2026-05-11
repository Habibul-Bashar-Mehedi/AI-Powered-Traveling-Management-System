package aptms.repositories;

import aptms.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing refresh tokens.
 * 
 * Requirements: FR-RFT-002, 4.2.2
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    /**
     * Find all non-revoked refresh tokens for a specific user.
     * Used to retrieve active sessions for a user.
     * 
     * @param userId the user's UUID
     * @return list of active refresh tokens
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    List<RefreshToken> findByUserIdAndRevokedAtIsNull(@Param("userId") UUID userId);
    
    /**
     * Delete all refresh tokens for a specific user.
     * Used during logout-all operations.
     * 
     * @param userId the user's UUID
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
    
    /**
     * Find a refresh token by its hash.
     * Used during token validation and refresh operations.
     * 
     * @param tokenHash the BCrypt hash of the token
     * @return optional containing the refresh token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * Delete all expired or revoked refresh tokens.
     * Used by scheduled cleanup job to remove old tokens.
     * 
     * @param now the current timestamp
     * @return the number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revokedAt IS NOT NULL")
    int deleteExpiredOrRevokedTokens(@Param("now") Instant now);
}

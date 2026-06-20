package aptms.repositories;

import aptms.entities.TokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing blacklisted tokens.
 * 
 * Requirements: FR-LGT-001, 4.2.3
 */
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, String> {
    
    /**
     * Find a blacklisted token by its JTI (JWT ID).
     * Used to check if a token has been blacklisted.
     * 
     * @param jti the JWT ID from the token
     * @return optional containing the blacklist entry if found
     */
    Optional<TokenBlacklist> findByJti(String jti);
    
    /**
     * Delete all blacklist entries for a specific user.
     * Used during user deletion to clean up associated tokens.
     * 
     * @param userId the user's UUID
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
    
    /**
     * Delete all blacklist entries that have expired.
     * This is used by a scheduled cleanup job to remove old entries.
     * 
     * @param now the current timestamp
     * @return the number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM TokenBlacklist tb WHERE tb.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}

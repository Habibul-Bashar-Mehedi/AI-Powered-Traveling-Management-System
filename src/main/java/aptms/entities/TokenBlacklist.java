package aptms.entities;

import aptms.enums.BlacklistReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity representing a blacklisted JWT token.
 * Blacklisted tokens are rejected even if they have valid signatures and are not expired.
 * 
 * Requirements: FR-LGT-001, 4.2.3
 */
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_blacklist_expires_at", columnList = "expires_at"),
    @Index(name = "idx_blacklist_user_id", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {
    
    /**
     * JWT ID (jti claim) from the token.
     * This is the unique identifier for the token.
     */
    @Id
    @Column(length = 36)
    private String jti;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id", columnDefinition = "BINARY(16)")
    private User user;
    
    /**
     * Reason why the token was blacklisted
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BlacklistReason reason;
    
    /**
     * Timestamp when the token expires.
     * After this time, the blacklist entry can be safely removed.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

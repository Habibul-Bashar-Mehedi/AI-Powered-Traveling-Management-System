package aptms.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a refresh token for JWT authentication.
 * Refresh tokens are used to obtain new access tokens without re-authentication.
 * 
 * Requirements: FR-RFT-002, 4.2.2
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at"),
    @Index(name = "idx_refresh_tokens_revoked_at", columnList = "revoked_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id", columnDefinition = "BINARY(16)")
    private User user;
    
    /**
     * BCrypt hash of the refresh token.
     * The actual token is never stored in plain text for security.
     */
    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;
    
    /**
     * Device information for audit trail (e.g., "iPhone 12", "Chrome on Windows")
     */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;
    
    /**
     * IP address from which the token was issued (IPv6 compatible)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent string from the client
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    /**
     * Timestamp when the token expires
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    /**
     * Timestamp when the token was revoked (null if still valid)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if the token is expired
     * @return true if the token has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the token has been revoked
     * @return true if the token has been revoked
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }
}

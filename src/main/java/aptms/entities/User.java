package aptms.entities;

import aptms.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a user account with JWT authentication support.
 * 
 * Requirements: FR-LGN-003, 4.2.1
 */
@Table(name = "users")
@Entity
@Audited
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    
    @Version
    private Integer version;
    
    @Column(nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;
    
    @Column(length = 100)
    private String countryId;
    
    /**
     * Counter for consecutive failed login attempts.
     * Reset to 0 on successful login.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;
    
    /**
     * Timestamp until which the account is locked due to too many failed login attempts.
     * Null if the account is not locked.
     */
    @Column(name = "lockout_until")
    private Instant lockoutUntil;
    
    /**
     * Timestamp of the last successful login.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    
    /**
     * Timestamp when the user was soft-deleted.
     * Null if the user is active.
     * Used for soft delete to maintain referential integrity while marking users as deleted.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if the account is currently locked.
     * @return true if the account is locked
     */
    public boolean isLocked() {
        return lockoutUntil != null && lockoutUntil.isAfter(Instant.now());
    }
}

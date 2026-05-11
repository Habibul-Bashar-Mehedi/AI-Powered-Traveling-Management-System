package aptms.enums;

/**
 * Enum representing the reason why a token was blacklisted.
 * 
 * Requirements: FR-LGT-001, 4.2.3
 */
public enum BlacklistReason {
    /**
     * Token was blacklisted due to user logout
     */
    LOGOUT,
    
    /**
     * Token was manually revoked by an administrator
     */
    REVOKED,
    
    /**
     * Token was blacklisted due to security concerns (e.g., suspicious activity)
     */
    SECURITY,
    
    /**
     * Token was blacklisted because the user changed their password
     */
    PASSWORD_CHANGE
}

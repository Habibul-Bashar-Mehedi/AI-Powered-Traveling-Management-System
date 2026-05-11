-- Migration: Create token_blacklist table
-- Version: V003
-- Description: Creates the token_blacklist table for storing blacklisted JWT tokens
-- Requirements: FR-LGT-001, 4.2.3

CREATE TABLE token_blacklist (
    jti VARCHAR(36) PRIMARY KEY COMMENT 'JWT ID (jti claim) from the token',
    user_id BINARY(16) NOT NULL COMMENT 'Foreign key to users table',
    reason VARCHAR(50) NOT NULL COMMENT 'Reason for blacklisting (LOGOUT, REVOKED, SECURITY, PASSWORD_CHANGE)',
    expires_at TIMESTAMP NOT NULL COMMENT 'Token expiration timestamp',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Blacklist entry creation timestamp',
    
    CONSTRAINT fk_blacklist_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_blacklist_expires_at (expires_at),
    INDEX idx_blacklist_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Blacklisted JWT tokens';

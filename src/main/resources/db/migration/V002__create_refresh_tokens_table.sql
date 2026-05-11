-- Migration: Create refresh_tokens table
-- Version: V002
-- Description: Creates the refresh_tokens table for storing refresh tokens
-- Requirements: FR-RFT-002, 4.2.2

CREATE TABLE refresh_tokens (
    id BINARY(16) PRIMARY KEY COMMENT 'UUID primary key',
    user_id BINARY(16) NOT NULL COMMENT 'Foreign key to users table',
    token_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt hash of the refresh token',
    device_info VARCHAR(255) COMMENT 'Device information for audit trail',
    ip_address VARCHAR(45) COMMENT 'IP address (IPv6 compatible)',
    user_agent TEXT COMMENT 'User agent string from the client',
    expires_at TIMESTAMP NOT NULL COMMENT 'Token expiration timestamp',
    revoked_at TIMESTAMP NULL COMMENT 'Token revocation timestamp (null if active)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT 'Last update timestamp',
    
    CONSTRAINT fk_refresh_tokens_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at),
    INDEX idx_refresh_tokens_revoked_at (revoked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Refresh tokens for JWT authentication';

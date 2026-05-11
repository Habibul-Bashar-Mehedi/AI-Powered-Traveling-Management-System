-- Migration: Add JWT authentication fields to users table
-- Version: V001
-- Description: Adds fields for JWT authentication including failed login tracking,
--              account lockout, last login timestamp, and audit timestamps
-- Requirements: FR-LGN-003, 3.4.1

-- Add new columns for JWT authentication
ALTER TABLE users
    ADD COLUMN failed_login_attempts INT DEFAULT 0 NOT NULL COMMENT 'Counter for consecutive failed login attempts',
    ADD COLUMN lockout_until TIMESTAMP NULL COMMENT 'Timestamp until which the account is locked',
    ADD COLUMN last_login_at TIMESTAMP NULL COMMENT 'Timestamp of the last successful login',
    ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT 'Account creation timestamp',
    ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT 'Last update timestamp';

-- Add index for lockout queries (partial index for better performance)
CREATE INDEX idx_users_lockout ON users(lockout_until)
    WHERE lockout_until IS NOT NULL;

-- Add index for email lookups (improves login performance)
CREATE INDEX idx_users_email ON users(email);

-- Add comment to table
ALTER TABLE users COMMENT = 'User accounts with JWT authentication support';

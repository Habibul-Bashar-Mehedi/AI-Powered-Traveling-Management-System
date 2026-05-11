-- Migration: Migrate users table ID from Long to UUID
-- Version: V000 (runs before other migrations)
-- Description: Converts the users table primary key from BIGINT to BINARY(16) UUID
--              and updates all foreign key references in related tables
-- Requirements: FR-LGN-003, 4.2.1

-- WARNING: This migration will modify the primary key of the users table and all related foreign keys.
-- Ensure you have a backup before running this migration.

-- ============================================
-- Step 1: Prepare users table
-- ============================================

-- Add a temporary UUID column to users
ALTER TABLE users ADD COLUMN id_uuid BINARY(16) NULL;

-- Generate UUIDs for existing users
-- Note: This uses UUID() function which generates UUID v1 (time-based)
UPDATE users SET id_uuid = UNHEX(REPLACE(UUID(), '-', ''));

-- Make the UUID column NOT NULL
ALTER TABLE users MODIFY COLUMN id_uuid BINARY(16) NOT NULL;

-- ============================================
-- Step 2: Update foreign key references in related tables
-- ============================================

-- Update booking table
ALTER TABLE booking ADD COLUMN user_id_uuid BINARY(16) NULL;
UPDATE booking b 
INNER JOIN users u ON b.user_id = u.id 
SET b.user_id_uuid = u.id_uuid;
ALTER TABLE booking MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;

-- Update chatHistories table
ALTER TABLE chatHistories ADD COLUMN user_id_uuid BINARY(16) NULL;
UPDATE chatHistories ch 
INNER JOIN users u ON ch.user_id = u.id 
SET ch.user_id_uuid = u.id_uuid;
ALTER TABLE chatHistories MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;

-- ============================================
-- Step 3: Drop old foreign key constraints
-- ============================================

-- Drop foreign key constraints (if they exist)
-- Note: Adjust constraint names if they differ in your database
SET @booking_fk = (SELECT CONSTRAINT_NAME 
                   FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
                   WHERE TABLE_NAME = 'booking' 
                   AND COLUMN_NAME = 'user_id' 
                   AND REFERENCED_TABLE_NAME = 'users' 
                   LIMIT 1);

SET @chat_fk = (SELECT CONSTRAINT_NAME 
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
                WHERE TABLE_NAME = 'chatHistories' 
                AND COLUMN_NAME = 'user_id' 
                AND REFERENCED_TABLE_NAME = 'users' 
                LIMIT 1);

SET @sql = IF(@booking_fk IS NOT NULL, 
              CONCAT('ALTER TABLE booking DROP FOREIGN KEY ', @booking_fk), 
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@chat_fk IS NOT NULL, 
              CONCAT('ALTER TABLE chatHistories DROP FOREIGN KEY ', @chat_fk), 
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- Step 4: Replace old columns with new UUID columns
-- ============================================

-- Update users table
ALTER TABLE users DROP PRIMARY KEY;
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users CHANGE COLUMN id_uuid id BINARY(16) NOT NULL;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Update booking table
ALTER TABLE booking DROP COLUMN user_id;
ALTER TABLE booking CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

-- Update chatHistories table
ALTER TABLE chatHistories DROP COLUMN user_id;
ALTER TABLE chatHistories CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

-- ============================================
-- Step 5: Re-create foreign key constraints
-- ============================================

ALTER TABLE booking 
    ADD CONSTRAINT fk_booking_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE chatHistories 
    ADD CONSTRAINT fk_chat_history_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ============================================
-- Step 6: Add indexes for foreign keys
-- ============================================

CREATE INDEX idx_booking_user_id ON booking(user_id);
CREATE INDEX idx_chat_history_user_id ON chatHistories(user_id);


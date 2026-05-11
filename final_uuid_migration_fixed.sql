-- Final steps of UUID migration (fixed)

-- Ensure booking user_id_uuid is populated
UPDATE booking b 
INNER JOIN users u ON b.user_id = u.id 
SET b.user_id_uuid = u.id_uuid
WHERE b.user_id_uuid IS NULL;

-- Make it NOT NULL
ALTER TABLE booking MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;

-- Drop foreign key constraint from booking
SET @booking_fk = (SELECT CONSTRAINT_NAME 
                   FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
                   WHERE TABLE_NAME = 'booking' 
                   AND COLUMN_NAME = 'user_id' 
                   AND REFERENCED_TABLE_NAME = 'users' 
                   LIMIT 1);

SET @sql = IF(@booking_fk IS NOT NULL, 
              CONCAT('ALTER TABLE booking DROP FOREIGN KEY ', @booking_fk), 
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove AUTO_INCREMENT from id column first
ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL;

-- Now drop primary key and old id column
ALTER TABLE users DROP PRIMARY KEY;
ALTER TABLE users DROP COLUMN id;

-- Rename id_uuid to id and make it primary key
ALTER TABLE users CHANGE COLUMN id_uuid id BINARY(16) NOT NULL;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Update booking table
ALTER TABLE booking DROP COLUMN user_id;
ALTER TABLE booking CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

-- Re-create foreign key constraint
ALTER TABLE booking 
    ADD CONSTRAINT fk_booking_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add index
CREATE INDEX idx_booking_user_id ON booking(user_id);

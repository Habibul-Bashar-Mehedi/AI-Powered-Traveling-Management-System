-- Comprehensive UUID migration

-- Drop all foreign keys referencing users
ALTER TABLE chat_histories DROP FOREIGN KEY FKngu1y7a7kjn021srngidmxniu;
ALTER TABLE hotels DROP FOREIGN KEY FKga2ffd7w8qgryll0pjxhxrclr;

-- Add UUID columns to all related tables
ALTER TABLE chat_histories ADD COLUMN IF NOT EXISTS user_id_uuid BINARY(16) NULL;
ALTER TABLE chat_histories_aud ADD COLUMN IF NOT EXISTS user_id_uuid BINARY(16) NULL;
ALTER TABLE hotels ADD COLUMN IF NOT EXISTS user_id_uuid BINARY(16) NULL;

-- Populate UUID columns
UPDATE chat_histories ch INNER JOIN users u ON ch.user_id = u.id SET ch.user_id_uuid = u.id_uuid WHERE ch.user_id_uuid IS NULL;
UPDATE chat_histories_aud cha INNER JOIN users u ON cha.user_id = u.id SET cha.user_id_uuid = u.id_uuid WHERE cha.user_id_uuid IS NULL;
UPDATE hotels h INNER JOIN users u ON h.user_id = u.id SET h.user_id_uuid = u.id_uuid WHERE h.user_id_uuid IS NULL;

-- Delete rows with NULL user_id (orphaned records)
DELETE FROM chat_histories WHERE user_id_uuid IS NULL;
DELETE FROM chat_histories_aud WHERE user_id_uuid IS NULL;
DELETE FROM hotels WHERE user_id_uuid IS NULL;

-- Make UUID columns NOT NULL
ALTER TABLE chat_histories MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;
ALTER TABLE chat_histories_aud MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;
ALTER TABLE hotels MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;

-- Migrate users table
ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE users DROP PRIMARY KEY;
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users CHANGE COLUMN id_uuid id BINARY(16) NOT NULL;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Update all related tables
ALTER TABLE booking DROP COLUMN user_id;
ALTER TABLE booking CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

ALTER TABLE chat_histories DROP COLUMN user_id;
ALTER TABLE chat_histories CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

ALTER TABLE chat_histories_aud DROP COLUMN user_id;
ALTER TABLE chat_histories_aud CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

ALTER TABLE hotels DROP COLUMN user_id;
ALTER TABLE hotels CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

-- Re-create foreign key constraints
ALTER TABLE booking ADD CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE chat_histories ADD CONSTRAINT fk_chat_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE hotels ADD CONSTRAINT fk_hotel_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_booking_user_id ON booking(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_history_user_id ON chat_histories(user_id);
CREATE INDEX IF NOT EXISTS idx_hotel_user_id ON hotels(user_id);

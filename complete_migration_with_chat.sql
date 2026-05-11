-- Complete UUID migration including chat_histories

-- Step 1: Add UUID columns to chat_histories tables
ALTER TABLE chat_histories ADD COLUMN user_id_uuid BINARY(16) NULL;
ALTER TABLE chat_histories_aud ADD COLUMN user_id_uuid BINARY(16) NULL;

-- Step 2: Populate UUID columns
UPDATE chat_histories ch 
INNER JOIN users u ON ch.user_id = u.id 
SET ch.user_id_uuid = u.id_uuid;

UPDATE chat_histories_aud cha 
INNER JOIN users u ON cha.user_id = u.id 
SET cha.user_id_uuid = u.id_uuid;

-- Step 3: Make UUID columns NOT NULL
ALTER TABLE chat_histories MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;
ALTER TABLE chat_histories_aud MODIFY COLUMN user_id_uuid BINARY(16) NOT NULL;

-- Step 4: Drop all foreign key constraints
ALTER TABLE chat_histories DROP FOREIGN KEY FKngu1y7a7kjn021srngidmxniu;

-- Step 5: Modify users table
ALTER TABLE users MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE users DROP PRIMARY KEY;
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users CHANGE COLUMN id_uuid id BINARY(16) NOT NULL;
ALTER TABLE users ADD PRIMARY KEY (id);

-- Step 6: Update all related tables
ALTER TABLE booking DROP COLUMN user_id;
ALTER TABLE booking CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

ALTER TABLE chat_histories DROP COLUMN user_id;
ALTER TABLE chat_histories CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

ALTER TABLE chat_histories_aud DROP COLUMN user_id;
ALTER TABLE chat_histories_aud CHANGE COLUMN user_id_uuid user_id BINARY(16) NOT NULL;

-- Step 7: Re-create foreign key constraints
ALTER TABLE booking 
    ADD CONSTRAINT fk_booking_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE chat_histories 
    ADD CONSTRAINT fk_chat_history_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Step 8: Add indexes
CREATE INDEX idx_booking_user_id ON booking(user_id);
CREATE INDEX idx_chat_history_user_id ON chat_histories(user_id);

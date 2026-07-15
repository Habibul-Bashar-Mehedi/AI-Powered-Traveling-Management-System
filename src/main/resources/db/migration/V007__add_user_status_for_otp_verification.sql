-- Adds account verification status for OTP-based email verification.
-- Existing rows default to ACTIVE (they predate the verification requirement).
ALTER TABLE users
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' AFTER role;

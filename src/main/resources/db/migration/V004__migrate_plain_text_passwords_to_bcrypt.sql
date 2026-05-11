-- Migration: Migrate plain text passwords to BCrypt hashes
-- Version: V004
-- Description: Identifies and logs users with plain text passwords.
--              The actual password hashing is performed by the PasswordMigrationService
--              to ensure proper BCrypt implementation.
-- Requirements: NFR-2 (Security - passwords must be BCrypt hashed)

-- ============================================
-- Step 1: Create audit log table for password migration
-- ============================================

CREATE TABLE IF NOT EXISTS password_migration_log (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    user_email VARCHAR(100) NOT NULL,
    migration_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    migration_status VARCHAR(20) NOT NULL, -- 'PENDING', 'COMPLETED', 'FAILED'
    notes TEXT,
    
    CONSTRAINT fk_password_migration_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_password_migration_user_id (user_id),
    INDEX idx_password_migration_status (migration_status),
    INDEX idx_password_migration_timestamp (migration_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Step 2: Log users with potential plain text passwords
-- ============================================

-- BCrypt hashes always start with $2a$, $2b$, or $2y$ and are 60 characters long
-- Any password not matching this pattern is considered plain text

INSERT INTO password_migration_log (id, user_id, user_email, migration_status, notes)
SELECT 
    UNHEX(REPLACE(UUID(), '-', '')) as id,
    u.id as user_id,
    u.email as user_email,
    'PENDING' as migration_status,
    CONCAT('Password length: ', LENGTH(u.password), 
           ', Starts with: ', SUBSTRING(u.password, 1, 4)) as notes
FROM users u
WHERE 
    -- Password does not start with BCrypt prefix
    (u.password NOT LIKE '$2a$%' 
     AND u.password NOT LIKE '$2b$%' 
     AND u.password NOT LIKE '$2y$%')
    -- Or password length is not 60 characters (BCrypt standard length)
    OR LENGTH(u.password) != 60;

-- ============================================
-- Step 3: Log migration summary
-- ============================================

-- This will be visible in Flyway migration logs
SELECT 
    COUNT(*) as total_users_requiring_migration,
    CONCAT('Found ', COUNT(*), ' user(s) with plain text passwords. ',
           'Run PasswordMigrationService.migrateAllPlainTextPasswords() to complete migration.') as migration_message
FROM password_migration_log
WHERE migration_status = 'PENDING';

-- ============================================
-- IMPORTANT NOTES:
-- ============================================
-- 
-- This migration script only IDENTIFIES and LOGS users with plain text passwords.
-- It does NOT perform the actual password hashing because:
-- 
-- 1. SQL cannot generate proper BCrypt hashes (requires Java BCryptPasswordEncoder)
-- 2. We need to maintain consistency with the application's BCrypt configuration
-- 3. We need proper error handling and logging for each password migration
-- 
-- TO COMPLETE THE MIGRATION:
-- 
-- After this migration runs, execute the following in your application:
-- 
--   @Autowired
--   private PasswordMigrationService passwordMigrationService;
--   
--   public void migratePasswords() {
--       int migratedCount = passwordMigrationService.migrateAllPlainTextPasswords();
--       logger.info("Migrated {} passwords", migratedCount);
--   }
-- 
-- Or create a REST endpoint for manual triggering:
-- 
--   @PostMapping("/admin/migrate-passwords")
--   @PreAuthorize("hasRole('ADMIN')")
--   public ResponseEntity<String> migratePasswords() {
--       int count = passwordMigrationService.migrateAllPlainTextPasswords();
--       return ResponseEntity.ok("Migrated " + count + " passwords");
--   }
-- 
-- ============================================

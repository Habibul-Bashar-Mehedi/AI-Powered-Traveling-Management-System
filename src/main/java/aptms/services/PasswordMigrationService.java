package aptms.services;

/**
 * Service for migrating plain text passwords to BCrypt hashes.
 * 
 * This service provides functionality to:
 * - Detect plain text passwords (not starting with $2a$)
 * - Hash plain text passwords using BCrypt
 * - Support password migration operations
 * 
 * Requirements: NFR-2 (Security - passwords must be BCrypt hashed)
 */
public interface PasswordMigrationService {
    
    /**
     * Check if a password is stored as plain text.
     * BCrypt hashes start with $2a$, $2b$, or $2y$ prefix.
     * 
     * @param password The password string to check
     * @return true if the password is plain text (not BCrypt hashed)
     */
    boolean isPlainTextPassword(String password);
    
    /**
     * Hash a plain text password using BCrypt.
     * 
     * @param plainTextPassword The plain text password to hash
     * @return BCrypt hash of the password
     * @throws IllegalArgumentException if plainTextPassword is null or empty
     */
    String hashPassword(String plainTextPassword);
    
    /**
     * Migrate all plain text passwords in the database to BCrypt hashes.
     * This method should be called during system maintenance or migration.
     * 
     * @return Number of passwords migrated
     */
    int migrateAllPlainTextPasswords();
}

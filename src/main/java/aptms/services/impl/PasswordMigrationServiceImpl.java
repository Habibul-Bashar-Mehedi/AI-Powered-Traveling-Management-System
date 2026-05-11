package aptms.services.impl;

import aptms.entities.User;
import aptms.repositories.UserRepository;
import aptms.services.PasswordMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of password migration service for converting plain text passwords to BCrypt hashes.
 * 
 * BCrypt password format:
 * - Starts with $2a$, $2b$, or $2y$ (algorithm identifier)
 * - Followed by cost factor (e.g., $10$)
 * - Followed by 22-character salt
 * - Followed by 31-character hash
 * - Total length: 60 characters
 * 
 * Example: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 * 
 * Requirements: NFR-2 (Security - passwords must be BCrypt hashed)
 */
@Service
public class PasswordMigrationServiceImpl implements PasswordMigrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationServiceImpl.class);
    
    /**
     * BCrypt hash pattern: starts with $2a$, $2b$, or $2y$
     * Followed by cost factor and salt/hash
     */
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    
    /**
     * BCrypt strength (cost factor) for password hashing.
     * Higher values = more secure but slower.
     * 10 is a good balance between security and performance.
     */
    private static final int BCRYPT_STRENGTH = 10;
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public PasswordMigrationServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
        logger.info("PasswordMigrationService initialized with BCrypt strength: {}", BCRYPT_STRENGTH);
    }
    
    @Override
    public boolean isPlainTextPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        // Check if password matches BCrypt pattern
        boolean isBCrypt = BCRYPT_PATTERN.matcher(password).matches();
        
        logger.debug("Password check: isBCrypt={}, length={}", isBCrypt, password.length());
        
        return !isBCrypt;
    }
    
    @Override
    public String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isEmpty()) {
            throw new IllegalArgumentException("Plain text password cannot be null or empty");
        }
        
        logger.debug("Hashing password (length: {})", plainTextPassword.length());
        
        String hash = passwordEncoder.encode(plainTextPassword);
        
        logger.debug("Password hashed successfully (hash length: {})", hash.length());
        
        return hash;
    }
    
    @Override
    @Transactional
    public int migrateAllPlainTextPasswords() {
        logger.info("Starting password migration: checking all users for plain text passwords");
        
        List<User> allUsers = userRepository.findAll();
        int migratedCount = 0;
        int totalUsers = allUsers.size();
        
        logger.info("Found {} users to check", totalUsers);
        
        for (User user : allUsers) {
            String currentPassword = user.getPassword();
            
            if (isPlainTextPassword(currentPassword)) {
                logger.warn("SECURITY: Plain text password detected for user: {} ({})", 
                    user.getId(), user.getEmail());
                
                // Hash the plain text password
                String hashedPassword = hashPassword(currentPassword);
                user.setPassword(hashedPassword);
                
                // Save the user with hashed password
                userRepository.save(user);
                
                migratedCount++;
                
                logger.info("Password migrated for user: {} ({}) - {}/{}", 
                    user.getId(), user.getEmail(), migratedCount, totalUsers);
            } else {
                logger.debug("User {} already has BCrypt password", user.getId());
            }
        }
        
        if (migratedCount > 0) {
            logger.warn("SECURITY: Password migration completed. {} out of {} passwords were plain text and have been hashed", 
                migratedCount, totalUsers);
        } else {
            logger.info("Password migration completed. All {} users already have BCrypt passwords", totalUsers);
        }
        
        return migratedCount;
    }
}

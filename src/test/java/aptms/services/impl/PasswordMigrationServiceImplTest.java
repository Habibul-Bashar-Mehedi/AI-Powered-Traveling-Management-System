package aptms.services.impl;

import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.repositories.UserRepository;
import aptms.services.PasswordMigrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordMigrationServiceImpl.
 * 
 * Tests password migration functionality including:
 * - Plain text password detection
 * - Password hashing with BCrypt
 * - Bulk password migration
 * 
 * Requirements: NFR-2 (Security - passwords must be BCrypt hashed)
 */
@ExtendWith(MockitoExtension.class)
class PasswordMigrationServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    private PasswordMigrationService passwordMigrationService;
    private BCryptPasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        passwordMigrationService = new PasswordMigrationServiceImpl(userRepository);
        passwordEncoder = new BCryptPasswordEncoder(10);
    }
    
    @Test
    void testIsPlainTextPassword_withPlainText_returnsTrue() {
        // Plain text passwords
        assertTrue(passwordMigrationService.isPlainTextPassword("password123"));
        assertTrue(passwordMigrationService.isPlainTextPassword("MySecurePassword!"));
        assertTrue(passwordMigrationService.isPlainTextPassword("12345678"));
    }
    
    @Test
    void testIsPlainTextPassword_withBCryptHash_returnsFalse() {
        // Valid BCrypt hashes (all start with $2a$, $2b$, or $2y$ and are 60 chars)
        String bcryptHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        assertFalse(passwordMigrationService.isPlainTextPassword(bcryptHash));
        
        String bcryptHash2b = "$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        assertFalse(passwordMigrationService.isPlainTextPassword(bcryptHash2b));
        
        String bcryptHash2y = "$2y$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        assertFalse(passwordMigrationService.isPlainTextPassword(bcryptHash2y));
    }
    
    @Test
    void testIsPlainTextPassword_withNullOrEmpty_returnsFalse() {
        assertFalse(passwordMigrationService.isPlainTextPassword(null));
        assertFalse(passwordMigrationService.isPlainTextPassword(""));
    }
    
    @Test
    void testHashPassword_withValidPassword_returnsBCryptHash() {
        String plainPassword = "MySecurePassword123!";
        
        String hashedPassword = passwordMigrationService.hashPassword(plainPassword);
        
        assertNotNull(hashedPassword);
        assertEquals(60, hashedPassword.length());
        assertTrue(hashedPassword.startsWith("$2a$") || 
                   hashedPassword.startsWith("$2b$") || 
                   hashedPassword.startsWith("$2y$"));
        
        // Verify the hash matches the original password
        assertTrue(passwordEncoder.matches(plainPassword, hashedPassword));
    }
    
    @Test
    void testHashPassword_withNullPassword_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordMigrationService.hashPassword(null);
        });
    }
    
    @Test
    void testHashPassword_withEmptyPassword_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordMigrationService.hashPassword("");
        });
    }
    
    @Test
    void testHashPassword_generatesDifferentHashesForSamePassword() {
        String plainPassword = "password123";
        
        String hash1 = passwordMigrationService.hashPassword(plainPassword);
        String hash2 = passwordMigrationService.hashPassword(plainPassword);
        
        // BCrypt uses random salt, so hashes should be different
        assertNotEquals(hash1, hash2);
        
        // But both should match the original password
        assertTrue(passwordEncoder.matches(plainPassword, hash1));
        assertTrue(passwordEncoder.matches(plainPassword, hash2));
    }
    
    @Test
    void testMigrateAllPlainTextPasswords_withPlainTextPasswords_migratesThem() {
        // Create users with plain text passwords
        User user1 = createUser("user1@example.com", "plainPassword1");
        User user2 = createUser("user2@example.com", "plainPassword2");
        User user3 = createUser("user3@example.com", 
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"); // Already BCrypt
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        int migratedCount = passwordMigrationService.migrateAllPlainTextPasswords();
        
        assertEquals(2, migratedCount);
        
        // Verify save was called twice (for user1 and user2)
        verify(userRepository, times(2)).save(any(User.class));
        
        // Capture the saved users
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        
        List<User> savedUsers = userCaptor.getAllValues();
        
        // Verify passwords were hashed
        for (User savedUser : savedUsers) {
            String password = savedUser.getPassword();
            assertNotNull(password);
            assertEquals(60, password.length());
            assertTrue(password.startsWith("$2a$") || 
                       password.startsWith("$2b$") || 
                       password.startsWith("$2y$"));
        }
    }
    
    @Test
    void testMigrateAllPlainTextPasswords_withAllBCryptPasswords_migratesNone() {
        // Create users with valid BCrypt passwords (must be exactly 60 characters)
        // These are real BCrypt hashes generated for testing
        User user1 = createUser("user1@example.com", 
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        User user2 = createUser("user2@example.com", 
            "$2a$10$rBV2HDeQcxWjVGa.Rc8mseIkgTZuuRqyHJbYxTY9jLEqvOZNj9Efa");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        
        int migratedCount = passwordMigrationService.migrateAllPlainTextPasswords();
        
        assertEquals(0, migratedCount);
        
        // Verify save was never called
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testMigrateAllPlainTextPasswords_withNoUsers_returnsZero() {
        when(userRepository.findAll()).thenReturn(List.of());
        
        int migratedCount = passwordMigrationService.migrateAllPlainTextPasswords();
        
        assertEquals(0, migratedCount);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testPasswordHashingRoundTrip_verifyOriginalPasswordMatches() {
        String originalPassword = "MySecurePassword123!";
        
        // Hash the password
        String hashedPassword = passwordMigrationService.hashPassword(originalPassword);
        
        // Verify original password matches
        assertTrue(passwordEncoder.matches(originalPassword, hashedPassword));
        
        // Verify different password doesn't match
        assertFalse(passwordEncoder.matches("WrongPassword", hashedPassword));
    }
    
    /**
     * Helper method to create a test user.
     */
    private User createUser(String email, String password) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(email.split("@")[0]);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(UserRole.USER);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}

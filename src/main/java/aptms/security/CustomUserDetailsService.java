package aptms.security;

import aptms.entities.User;
import aptms.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetailsService implementation for JWT authentication.
 * 
 * Loads user by UUID (not username) since JWT tokens contain user ID in the 'sub' claim.
 * Returns Spring Security UserDetails with:
 * - username: User UUID as string
 * - password: BCrypt hashed password
 * - authorities: User roles
 * - accountLocked: Based on lockoutUntil timestamp
 * 
 * Requirements: 3.1.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Load user by username (which is actually the user UUID for JWT authentication).
     * 
     * @param userId User UUID as string
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);
        
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format: {}", userId);
            throw new UsernameNotFoundException("Invalid user ID format: " + userId);
        }
        
        User user = userRepository.findById(userUuid)
            .orElseThrow(() -> {
                log.warn("User not found with ID: {}", userId);
                return new UsernameNotFoundException("User not found: " + userId);
            });
        
        log.debug("User found: {} ({})", user.getUsername(), user.getEmail());
        
        // Check if account is locked
        boolean accountLocked = user.getLockoutUntil() != null && 
                               user.getLockoutUntil().isAfter(Instant.now());
        
        if (accountLocked) {
            log.warn("Account is locked until: {}", user.getLockoutUntil());
        }
        
        // Build UserDetails with user ID as username (for JWT authentication)
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getId().toString())
            .password(user.getPassword())
            .authorities(Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            ))
            .accountLocked(accountLocked)
            .accountExpired(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }
}

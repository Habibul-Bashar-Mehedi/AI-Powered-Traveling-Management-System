package aptms.api;

import aptms.services.PasswordMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for administrative operations.
 * 
 * Provides endpoints for:
 * - Password migration from plain text to BCrypt
 * - Other administrative tasks
 * 
 * All endpoints require ADMIN role.
 * 
 * Requirements: NFR-2 (Security - passwords must be BCrypt hashed)
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final PasswordMigrationService passwordMigrationService;
    
    public AdminController(PasswordMigrationService passwordMigrationService) {
        this.passwordMigrationService = passwordMigrationService;
    }
    
    /**
     * Migrate all plain text passwords to BCrypt hashes.
     * 
     * This endpoint should be called after deploying the password migration feature
     * to convert any existing plain text passwords to BCrypt hashes.
     * 
     * @return Response with number of passwords migrated
     */
    @PostMapping("/migrate-passwords")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> migratePasswords() {
        logger.info("Password migration triggered by admin");
        
        try {
            int migratedCount = passwordMigrationService.migrateAllPlainTextPasswords();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("migratedCount", migratedCount);
            response.put("message", String.format("Successfully migrated %d password(s) to BCrypt", migratedCount));
            
            logger.info("Password migration completed: {} passwords migrated", migratedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Password migration failed", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Password migration failed");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

package aptms.api;

import aptms.services.PasswordMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for administrative operations.
 * All endpoints require ADMIN or SUPER_ADMIN role.
 * Requirements: NFR-2 (Security - passwords must be BCrypt hashed)
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final PasswordMigrationService passwordMigrationService;

    @PostMapping("/migrate-passwords")
    public ResponseEntity<Map<String, Object>> migratePasswords() {
        log.info("Password migration triggered by admin");
        try {
            int migratedCount = passwordMigrationService.migrateAllPlainTextPasswords();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("migratedCount", migratedCount);
            response.put("message",
                    String.format("Successfully migrated %d password(s) to BCrypt", migratedCount));
            log.info("Password migration completed: {} passwords migrated", migratedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Password migration failed", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Password migration failed");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

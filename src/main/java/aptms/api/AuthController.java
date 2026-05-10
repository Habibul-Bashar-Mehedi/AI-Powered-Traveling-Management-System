package aptms.api;

import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.services.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/auth/user")
public class AuthController {
    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> userRegister(@RequestBody User user) {
        User registeredUser = registrationService.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> userLogin(@RequestBody User loginRequest) {
        String response = registrationService.loginUser(
            loginRequest.getEmail(), 
            loginRequest.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = registrationService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable long id, @RequestBody UserUpdateRequest request) {
        boolean updated = registrationService.updateUser(
            id, 
            request.getUsername(), 
            request.getEmail(), 
            request.getPassword(), 
            request.getRole(), 
            request.getCountryId()
        );
        
        if (updated) {
            return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, USER));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format(ENTITY_NOT_FOUND_MESSAGE, USER, id));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable long id) {
        String result = registrationService.deleteUser(id);
        return ResponseEntity.ok(result);
    }
    
    // Inner class for update request
    public static class UserUpdateRequest {
        private String username;
        private String email;
        private String password;
        private UserRole role;
        private String countryId;
        
        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public String getCountryId() { return countryId; }
        public void setCountryId(String countryId) { this.countryId = countryId; }
    }
}

package aptms.api;

import aptms.entities.User;
import aptms.services.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth/user")
public class AuthController {
    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/registar")
    public User userRegister(@RequestBody User user) {
        return registrationService.registerUser(user);
    }

    @PostMapping("/login")
    public String userLogin(@RequestBody User loginRequest) {
        return registrationService.loginUser(loginRequest.getEmail(),loginRequest.getPassword());
    }

    @GetMapping()
    public List<User> getAllUsers() {
        return registrationService.getAllUsers();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable long id, @RequestBody User user) {

        boolean update = registrationService.updateUser(id,user.getUsername(), user.getEmail(), user.getPassword(), user.getRole(),user.getCountryId());
        if(update) {
            return ResponseEntity.ok("user updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("user not found with id : "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable long id) {
        String result = registrationService.deleteUser(id);
        if(result.equals("user is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }


}

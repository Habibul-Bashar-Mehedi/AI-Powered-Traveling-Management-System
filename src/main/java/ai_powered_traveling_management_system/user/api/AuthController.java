package ai_powered_traveling_management_system.user.api;

import ai_powered_traveling_management_system.user.entities.User;
import ai_powered_traveling_management_system.user.services.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/registar")
    public User userRegister(@RequestBody User user) {
        return registrationService.registerUser(user);
    }

    @PostMapping("/login")
    public String userLogin(@RequestBody User loginRequest) {
        return registrationService.loginUser(loginRequest.getEmail(),loginRequest.getPassword());
    }

}

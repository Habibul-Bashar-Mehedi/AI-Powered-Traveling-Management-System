package ai_powered_traveling_management_system.user.services;

import ai_powered_traveling_management_system.user.entities.User;
import ai_powered_traveling_management_system.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistrationService {
    @Autowired
    private UserRepository userRepository;

    public User registerUser(User user) {
        if(userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        return userRepository.save(user);
    }

    public String loginUser(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getPassword().equals(password)) {
                        return "Login Successful! Welcome " + user.getUsername();
                    }
                    return "Invalid password";
                })
                .orElse("User not found with this email");
    }

    public List<User> getAllUsers () {
        return userRepository.findAll();
    }


}

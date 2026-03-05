package ai_powered_traveling_management_system.user.service;

import ai_powered_traveling_management_system.user.entities.User;
import ai_powered_traveling_management_system.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RegistrationService {
    @Autowired
    private UserRepository userRepository;

    public User registerUser(User user) {
        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exits");
        }
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public String loginUser(String email,String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isPresent()) {
            User user = userOptional.get();
            if(user.getPassword().equals(password)) {
                return "Login Successful! Welcome "+user.getUsername();
            }else {
                return "invalid username or password";
            }
        }
        return "user not found of this email";
    }

}

package aptms.services;

import aptms.entities.User;
import aptms.repositories.UserRepository;
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

    //user
    public String deleteUser(long id) {
        if(!userRepository.existsById(id)) {
            return "user not found";
        }
        userRepository.deleteById(id);
        return "user is deleted";
    }

    public boolean updateUser(
            long id,String username,
            String email,String password,
            String role,String countryId) {

        return userRepository.findById(id).map(user -> {
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setRole(role);
            user.setCountryId(countryId);

            userRepository.save(user);
            return true;
        }).orElse(false);
    }
}

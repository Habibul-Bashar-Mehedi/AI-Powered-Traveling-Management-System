package aptms.services;

import aptms.entities.User;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistrationService {
    private final UserRepository userRepository;

    public RegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user) {
        if(userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateValueFoundExceptions("Email already exists");
        }
        return userRepository.save(user);
    }

    public String loginUser(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getPassword().equals(password)) {
                        return "Login Successful! Welcome " + user.getUsername();
                    }
                    throw new InvalidException("Invalid password");
                })
                .orElseThrow(()->
                        new InvalidException("User not found with this email. invalid email"));
    }

    public List<User> getAllUsers () {
        return userRepository.findAll();
    }

    //user
    public String deleteUser(long id) {
        if(!userRepository.existsById(id)) {
            throw new IdNotFoundException("user id not found");
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
        }).orElseThrow(()->
                new IdNotFoundException("user id not found")
        );
    }
}

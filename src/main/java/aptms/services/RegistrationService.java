package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.SecurityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class RegistrationService {
    private final UserRepository userRepository;

    public RegistrationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User registerUser(User user) {
        if(userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateValueFoundExceptions(
                String.format(DUPLICATE_ENTRY_MESSAGE, FIELD_EMAIL)
            );
        }
        
        // Set default role if not provided
        if(user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        
        return userRepository.save(user);
    }

    public String loginUser(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (user.getPassword().equals(password)) {
                        return "Login Successful! Welcome " + user.getUsername();
                    }
                    throw new InvalidException(INVALID_CREDENTIALS_MESSAGE);
                })
                .orElseThrow(() ->
                        new InvalidException(USER_NOT_FOUND_MESSAGE));
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteUser(long id) {
        if(!userRepository.existsById(id)) {
            throw new IdNotFoundException(
                String.format(ENTITY_NOT_FOUND_MESSAGE, USER, id)
            );
        }
        userRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, USER);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateUser(
            long id, String username,
            String email, String password,
            UserRole role, String countryId) {

        return userRepository.findById(id).map(user -> {
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setRole(role);
            user.setCountryId(countryId);

            userRepository.save(user);
            return true;
        }).orElseThrow(() ->
                new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, USER, id)
                )
        );
    }
}

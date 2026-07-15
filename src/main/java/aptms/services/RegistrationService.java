package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.User;
import aptms.enums.UserRole;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.SecurityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class RegistrationService {
    private static final int MAX_LIST_SIZE = 500;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public String loginUser(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (passwordEncoder.matches(password, user.getPassword())) {
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
        return userRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteUser(UUID id) {
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
            UUID id, String username,
            String email, String password,
            UserRole role, String countryId) {

        return userRepository.findById(id).map(user -> {
            user.setUsername(username);
            if (password != null && !password.isBlank()) {
                user.setPassword(passwordEncoder.encode(password));
            }
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

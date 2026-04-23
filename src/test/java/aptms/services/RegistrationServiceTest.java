package aptms.services;

import aptms.entities.User;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RegistrationService registrationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("password123");
        testUser.setRole("USER");
        testUser.setCountryId("BD");
    }

    @Test
    void registerUserTest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = registrationService.registerUser(testUser);

        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void loginUserSuccessTest() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        String result = registrationService.loginUser("john@example.com", "password123");

        assertEquals("Login Successful! Welcome john_doe", result);
    }

    @Test
    void alreadyExistsEmailTest() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> registrationService.registerUser(testUser));
    }

    @Test
    void getAllUsersTest() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        List<User> users = registrationService.getAllUsers();
        assertEquals(1, users.size());
    }

    @Test
    void deleteUserTest() {
        when(userRepository.existsById(1L)).thenReturn(true);

        String response = registrationService.deleteUser(1L);

        assertEquals("user is deleted", response);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void updateUserTest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        boolean result = registrationService.updateUser(
                1L, "new_name", "new@example.com", "newPass", "ADMIN", "US"
        );

        assertTrue(result);
        assertEquals("new_name", testUser.getUsername());
        assertEquals("new@example.com", testUser.getEmail());
        verify(userRepository, times(1)).save(testUser);
    }
}
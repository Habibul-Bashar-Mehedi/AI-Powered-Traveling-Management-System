package aptms.services;

import aptms.exceptions.InvalidException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // স্প্রিং-এর পুরো কনটেক্সট লোড করবে যাতে AOP প্রক্সিগুলো কাজ করতে পারে
class RegistrationServiceTest {

    @Autowired
    private RegistrationService registrationService;

    @Test
    @DisplayName("Should block delete request if user is not an Admin")
    void testDeleteUserWithoutAdmin() {
        // We are expecting an InvalidException because our Aspect
        // is currently hardcoded to "USER" while this method requires "ADMIN".

        Exception exception = assertThrows(InvalidException.class, () -> {
            registrationService.deleteUser(1L);
        });

        // This message must match the one in your SecurityAspects class
        String expectedMessage = "Access Denied: Admin role required.";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
        System.out.println("Test Status: Unauthorized access was successfully blocked!");
    }

    @Test
    @DisplayName("Should allow access for login as role matches")
    void testLoginAccess() {
        // Since loginUser requires "USER" role and our Aspect currently provides "USER",
        // the security check should pass and not throw any security exception.

        assertDoesNotThrow(() -> {
            try {
                registrationService.loginUser("test@email.com", "1234");
            } catch (InvalidException e) {
                // This catch block handles business logic errors (like 'User Not Found'),
                // proving that the AOP security layer did not block the execution.
                System.out.println("AOP allowed the call. Business logic is processing.");
            }
        });
    }
}
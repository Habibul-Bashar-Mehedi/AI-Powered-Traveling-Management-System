package aptms.aspects;

import aptms.annotations.SecureAction;
import aptms.exceptions.InvalidException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import static aptms.constants.SecurityConstants.*;

@Aspect
@Component
public class SecurityAspects {
    
    @Before("@annotation(secureAction)")
    public void authorize(SecureAction secureAction) {
        System.out.println(">>> AOP Security check: necessary role is " + secureAction.role());

        // TODO: Replace with actual authentication context
        String currentLoggingRole = ROLE_USER; // Temporary - should come from SecurityContext

        if(secureAction.role().equals(ROLE_ADMIN) && !currentLoggingRole.equals(ROLE_ADMIN)) {
            System.out.println(">>> AOP security check: Access Denied!");
            throw new InvalidException(ACCESS_DENIED_MESSAGE);
        }

        System.out.println(">>> AOP security check: permission granted");
    }
}

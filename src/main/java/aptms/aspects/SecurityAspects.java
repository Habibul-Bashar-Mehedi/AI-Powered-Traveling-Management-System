package aptms.aspects;

import aptms.annotations.SecureAction;
import aptms.exceptions.InvalidException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SecurityAspects {
    @Before("@annotation(secureAction)")
    public void authorize(SecureAction secureAction ) {
        System.out.println(">>> AOP Security check: necessary role is " + secureAction.role());

        String currentLoggingRole = "USER"; // temp data

        if(secureAction.role().equals("ADMIN") && !currentLoggingRole.equals("ADMIN")) {
            System.out.println(">>> AOP security check: Access Denied!");
            // এই মেসেজটি হুবহু টেস্ট ক্লাসে থাকতে হবে
            throw new InvalidException("Access Denied: Admin role required.");
        }

        System.out.println(">>> AOP security check: permission granted");
    }
}

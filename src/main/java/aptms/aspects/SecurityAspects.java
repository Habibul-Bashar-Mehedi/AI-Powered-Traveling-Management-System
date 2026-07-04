package aptms.aspects;

import aptms.annotations.SecureAction;
import aptms.exceptions.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import static aptms.constants.SecurityConstants.*;

@Aspect
@Component
@Slf4j
public class SecurityAspects {

    @Before("@annotation(secureAction)")
    public void authorize(SecureAction secureAction) {
        log.debug("AOP security check: necessary role is {}", secureAction.role());

        if (secureAction.role().equals(ROLE_ADMIN) && !currentUserHasAdminRole()) {
            log.warn("AOP security check: Access Denied!");
            throw new InvalidException(ACCESS_DENIED_MESSAGE);
        }

        log.debug("AOP security check: permission granted");
    }

    private boolean currentUserHasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_" + ROLE_ADMIN) || role.equals("ROLE_SUPER_ADMIN")) {
                return true;
            }
        }
        return false;
    }
}

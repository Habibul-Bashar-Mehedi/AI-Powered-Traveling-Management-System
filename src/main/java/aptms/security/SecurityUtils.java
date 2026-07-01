package aptms.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Shared utility for resolving the authenticated user's UUID from the
 * SecurityContext.
 *
 * <p>Centralises the logic that was previously copy-pasted into every controller,
 * ensuring a consistent, safe extraction:
 * <ul>
 *   <li>Returns 401 for null / anonymous / non-UUID principals instead of 500.</li>
 *   <li>Handles both UserDetails principals (JWT path) and plain-string
 *       principals (session path) transparently.</li>
 * </ul>
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Resolve the UUID of the currently authenticated user.
     *
     * @return the user's UUID
     * @throws ResponseStatusException 401 if not authenticated, anonymous,
     *                                 or the principal cannot be parsed as UUID
     */
    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }
        try {
            Object principal = auth.getPrincipal();
            // JWT path: principal is a UserDetails object whose username is the UUID string
            String name = (principal instanceof UserDetails ud)
                    ? ud.getUsername()
                    : auth.getName();
            return UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Cannot resolve user identity from authentication principal");
        }
    }
}


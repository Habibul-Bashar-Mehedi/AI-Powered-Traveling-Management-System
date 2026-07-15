package aptms.exceptions;

/**
 * Thrown when login is attempted on an account still in PENDING_VERIFICATION status.
 */
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}

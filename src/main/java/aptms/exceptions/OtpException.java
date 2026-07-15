package aptms.exceptions;

/**
 * Thrown for all OTP verification/resend failures.
 * The errorCode drives the HTTP status + error code in GlobalExceptionHandler.
 */
public class OtpException extends RuntimeException {

    public enum ErrorCode {
        OTP_EXPIRED,
        OTP_INVALID,
        OTP_MAX_ATTEMPTS,
        OTP_RESEND_COOLDOWN
    }

    private final ErrorCode errorCode;

    public OtpException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

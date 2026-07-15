package aptms.services;

/**
 * Generates, stores (Redis-backed, TTL-based), and verifies 6-digit OTP codes
 * used for email verification during registration.
 */
public interface OtpService {

    /**
     * Generate a new OTP for the given email, store it in Redis, reset the
     * attempt counter, and send it via email.
     */
    void generateAndSend(String email);

    /**
     * Verify the given code against the stored OTP for this email.
     * Throws OtpException (OTP_EXPIRED / OTP_INVALID / OTP_MAX_ATTEMPTS) on failure.
     * Clears all OTP state for the email on success.
     */
    void verify(String email, String code);

    /**
     * Whether a new OTP may be sent to this email (resend cooldown has elapsed).
     */
    boolean canResend(String email);

    /**
     * Mark that an OTP was just (re)sent, starting the resend cooldown.
     */
    void markResent(String email);
}

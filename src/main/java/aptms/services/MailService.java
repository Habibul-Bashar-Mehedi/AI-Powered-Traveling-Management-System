package aptms.services;

/**
 * Outbound transactional email.
 */
public interface MailService {

    /**
     * Send a registration OTP verification code to the given address.
     */
    void sendOtpEmail(String toEmail, String otp);
}

package aptms.services.impl;

import aptms.services.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final int otpTtlMinutes;

    public MailServiceImpl(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.otp.ttl-minutes}") int otpTtlMinutes) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.otpTtlMinutes = otpTtlMinutes;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Verify your SMTS account");
        message.setText(
            "Your verification code is: " + otp + "\n\n" +
            "This code expires in " + otpTtlMinutes + " minutes. " +
            "If you didn't request this, you can safely ignore this email.");

        try {
            mailSender.send(message);
            logger.info("OTP email dispatched to {}", toEmail);
        } catch (Exception e) {
            // Don't fail registration/resend on SMTP misconfiguration or downtime —
            // the OTP is still valid in Redis and can be redelivered via resend-otp
            // once mail is fixed. Never log the OTP value itself.
            logger.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
}

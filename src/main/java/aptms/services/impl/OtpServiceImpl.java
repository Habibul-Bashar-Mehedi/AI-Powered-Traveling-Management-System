package aptms.services.impl;

import aptms.entities.OtpVerification;
import aptms.exceptions.OtpException;
import aptms.repositories.OtpVerificationRepository;
import aptms.services.MailService;
import aptms.services.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Database-backed OTP generation/verification for email verification.
 *
 * OTP records are stored in the otp_verifications table (one row per email).
 * Verification, expiry, attempts, and resend cooldown are all managed via the DB.
 */
@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final OtpVerificationRepository otpVerificationRepository;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    private final long ttlMinutes;
    private final int maxAttempts;
    private final long resendCooldownSeconds;

    public OtpServiceImpl(
            OtpVerificationRepository otpVerificationRepository,
            MailService mailService,
            @Value("${app.otp.ttl-minutes}") long otpTtlMinutes,
            @Value("${app.otp.max-attempts}") int maxAttempts,
            @Value("${app.otp.resend-cooldown-seconds}") long resendCooldownSeconds) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.mailService = mailService;
        this.ttlMinutes = otpTtlMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    @Override
    @Transactional
    public void generateAndSend(String email) {
        String otp = generateCode();

        // Remove any existing pending OTP for this email, then create a fresh one.
        otpVerificationRepository.findByEmail(email).ifPresent(otpVerificationRepository::delete);

        OtpVerification verification = new OtpVerification();
        verification.setEmail(email);
        verification.setOtpCode(otp);
        verification.setAttempts(0);
        verification.setExpiresAt(Instant.now().plusSeconds(ttlMinutes * 60));
        verification.setCreatedAt(Instant.now());
        verification.setUpdatedAt(Instant.now());
        otpVerificationRepository.save(verification);

        mailService.sendOtpEmail(email, otp);
        logger.info("OTP generated and dispatched for {}", email);
    }

    @Override
    @Transactional
    public void verify(String email, String code) {
        OtpVerification verification = otpVerificationRepository.findByEmail(email)
            .orElseThrow(() -> new OtpException(OtpException.ErrorCode.OTP_EXPIRED,
                "Verification code has expired or does not exist. Please request a new one."));

        if (verification.isVerified() || verification.isExpired()) {
            throw new OtpException(OtpException.ErrorCode.OTP_EXPIRED,
                "Verification code has expired or does not exist. Please request a new one.");
        }

        if (!verification.getOtpCode().equals(code)) {
            int attempts = verification.getAttempts() + 1;
            verification.setAttempts(attempts);
            verification.setUpdatedAt(Instant.now());
            otpVerificationRepository.save(verification);

            if (attempts >= maxAttempts) {
                // Delete the record — force a fresh resend.
                otpVerificationRepository.delete(verification);
                logger.warn("Max OTP attempts exceeded for {}", email);
                throw new OtpException(OtpException.ErrorCode.OTP_MAX_ATTEMPTS,
                    "Too many incorrect attempts. Please request a new verification code.");
            }
            throw new OtpException(OtpException.ErrorCode.OTP_INVALID,
                "Invalid verification code.");
        }

        // Success.
        verification.setVerifiedAt(Instant.now());
        verification.setUpdatedAt(Instant.now());
        otpVerificationRepository.save(verification);
        logger.info("OTP verified successfully for {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canResend(String email) {
        return otpVerificationRepository.findByEmail(email)
            .map(v -> {
                if (v.getLastResendAt() == null) return true;
                return v.getLastResendAt().plusSeconds(resendCooldownSeconds).isBefore(Instant.now());
            })
            .orElse(true);
    }

    @Override
    @Transactional
    public void markResent(String email) {
        otpVerificationRepository.findByEmail(email).ifPresent(v -> {
            v.setLastResendAt(Instant.now());
            v.setUpdatedAt(Instant.now());
            otpVerificationRepository.save(v);
        });
    }

    private String generateCode() {
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}

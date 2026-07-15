package aptms.services;

import aptms.dto.AuthResponse;
import aptms.dto.LoginRequest;
import aptms.dto.RegisterRequest;
import aptms.dto.RegisterResponse;
import aptms.dto.ResendOtpRequest;
import aptms.dto.VerifyOtpRequest;

import java.util.UUID;

/**
 * Service interface for authentication operations.
 * 
 * Handles user registration, login, token refresh, and logout operations
 * with JWT token management and security features like account lockout.
 * 
 * Requirements: FR-REG-001, FR-LGN-001, FR-LGN-003, FR-RFT-001, FR-LGT-001, FR-LGT-003, 3.1.4
 */
public interface AuthenticationService {
    
    /**
     * Register new user in PENDING_VERIFICATION status and send an OTP verification email.
     * No tokens are issued until the OTP is verified via {@link #verifyOtp}.
     *
     * @param request Registration request DTO
     * @return Registration response confirming the account was created and an OTP was sent
     * @throws aptms.exceptions.DuplicateValueFoundExceptions if email already exists
     * @throws IllegalArgumentException if validation fails
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Verify a registration OTP and activate the account, issuing tokens on success.
     *
     * @param request Email + OTP code
     * @return Authentication response with user info and tokens
     * @throws aptms.exceptions.OtpException if the code is invalid, expired, or attempts are exhausted
     */
    AuthResponse verifyOtp(VerifyOtpRequest request);

    /**
     * Resend a registration OTP, subject to a cooldown.
     *
     * @param request Email to resend the OTP to
     * @throws aptms.exceptions.OtpException if the resend cooldown hasn't elapsed
     */
    void resendOtp(ResendOtpRequest request);

    /**
     * Authenticate user and issue tokens.
     * 
     * Performs the following operations:
     * 1. Find user by email
     * 2. Check account lockout status
     * 3. Verify password with BCrypt
     * 4. Handle failed login attempts (increment counter, trigger lockout after 5 failures)
     * 5. Reset failed attempts counter on successful login
     * 6. Update lastLoginAt timestamp
     * 7. Generate access token and refresh token
     * 8. Store refresh token in database
     * 9. Return authentication response with tokens
     * 
     * @param request Login request DTO
     * @return Authentication response with user info and tokens
     * @throws aptms.exceptions.InvalidException if credentials are invalid
     * @throws aptms.exceptions.InvalidException if account is locked (HTTP 423)
     * @throws aptms.exceptions.EmailNotVerifiedException if the account is pending email verification (HTTP 403)
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * Refresh access token using refresh token.
     * 
     * Performs the following operations:
     * 1. Validate refresh token (exists, not expired, not revoked)
     * 2. Detect refresh token reuse (security event)
     * 3. Rotate refresh token (invalidate old, create new)
     * 4. Generate new access token
     * 5. Return authentication response with new tokens
     * 
     * If reuse is detected, ALL user's refresh tokens are revoked.
     * 
     * @param refreshToken Refresh token string
     * @return Authentication response with new tokens
     * @throws aptms.exceptions.InvalidException if refresh token is invalid, expired, or revoked
     * @throws aptms.exceptions.InvalidException if refresh token reuse is detected
     */
    AuthResponse refreshToken(String refreshToken);
    
    /**
     * Logout user and revoke tokens.
     * 
     * Performs the following operations:
     * 1. Extract jti from access token
     * 2. Add jti to blacklist with TTL = token's remaining lifetime
     * 3. Delete user's refresh token from database
     * 
     * @param accessToken Access token string (JWT)
     * @param userId User UUID
     * @throws IllegalArgumentException if accessToken or userId is null
     */
    void logout(String accessToken, UUID userId);
    
    /**
     * Logout from all devices.
     * 
     * Revokes all refresh tokens for the user, terminating all active sessions.
     * 
     * @param userId User UUID
     * @throws IllegalArgumentException if userId is null
     */
    void logoutAll(UUID userId);
}

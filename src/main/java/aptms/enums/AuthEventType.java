package aptms.enums;

/**
 * Authentication event types for structured logging.
 * 
 * Requirements: NFR-2, SEC-4
 */
public enum AuthEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    REGISTRATION_SUCCESS,
    REGISTRATION_FAILURE,
    TOKEN_REFRESH,
    LOGOUT,
    LOGOUT_ALL,
    ACCOUNT_LOCKOUT,
    REFRESH_TOKEN_REUSE,
    TOKEN_VALIDATION_FAILURE
}

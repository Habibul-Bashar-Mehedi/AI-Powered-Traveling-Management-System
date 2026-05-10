package aptms.constants;

public final class SecurityConstants {
    
    private SecurityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Role Constants
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VENDOR = "VENDOR";

    // Security Messages
    public static final String ACCESS_DENIED_MESSAGE = "Access Denied: Admin role required.";
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";
    public static final String USER_NOT_FOUND_MESSAGE = "User not found with this email";
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized access";

    // JWT Constants
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String TOKEN_TYPE = "JWT";
}

package aptms.constants;

public final class ValidationConstants {
    
    private ValidationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Validation Messages
    public static final String REQUIRED_FIELD_MESSAGE = "%s is required";
    public static final String INVALID_EMAIL_MESSAGE = "Invalid email format";
    public static final String INVALID_PASSWORD_MESSAGE = "Password must be at least %d characters";
    public static final String INVALID_FIELD_MESSAGE = "Invalid %s";
    public static final String FIELD_TOO_SHORT_MESSAGE = "%s must be at least %d characters";
    public static final String FIELD_TOO_LONG_MESSAGE = "%s must not exceed %d characters";
    public static final String INVALID_DATE_RANGE_MESSAGE = "Check-out date must be after check-in date";
    public static final String INVALID_GUEST_COUNT_MESSAGE = "Guest count must be at least 1";
    public static final String INVALID_PRICE_MESSAGE = "Price must be greater than 0";

    // Field Names
    public static final String FIELD_EMAIL = "Email";
    public static final String FIELD_PASSWORD = "Password";
    public static final String FIELD_USERNAME = "Username";
    public static final String FIELD_NAME = "Name";
    public static final String FIELD_ADDRESS = "Address";
    public static final String FIELD_REGION = "Region";
    public static final String FIELD_DESCRIPTION = "Description";

    // Validation Constraints
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MAX_EMAIL_LENGTH = 100;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_ADDRESS_LENGTH = 255;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;
}

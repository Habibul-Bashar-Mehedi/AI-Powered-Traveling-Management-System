package aptms.constants;

public final class BookingConstants {
    
    private BookingConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Booking Status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String STATUS_CHECKED_OUT = "CHECKED_OUT";

    // Booking Messages
    public static final String BOOKING_CREATED_MESSAGE = "Booking created successfully";
    public static final String BOOKING_UPDATED_MESSAGE = "Booking updated successfully";
    public static final String BOOKING_DELETED_MESSAGE = "Booking deleted successfully";
    public static final String BOOKING_NOT_FOUND_MESSAGE = "Booking not found with id: ";
    public static final String ROOM_ALREADY_BOOKED_MESSAGE = "This room is already booked for the selected dates!";
    public static final String BOOKING_VALIDATION_ERROR = "User, Room, and Hotel information are required!";
}

package aptms.constants;

public final class EntityConstants {
    
    private EntityConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Generic Messages
    public static final String ENTITY_CREATED_MESSAGE = "%s created successfully";
    public static final String ENTITY_UPDATED_MESSAGE = "%s updated successfully";
    public static final String ENTITY_DELETED_MESSAGE = "%s deleted successfully";
    public static final String ENTITY_NOT_FOUND_MESSAGE = "%s not found with id: %s";
    public static final String ENTITY_ALREADY_EXISTS_MESSAGE = "%s already exists";
    public static final String DUPLICATE_ENTRY_MESSAGE = "Duplicate entry found for %s";

    // Entity Names
    public static final String USER = "User";
    public static final String BOOKING = "Booking";
    public static final String HOTEL = "Hotel";
    public static final String ROOM = "Room";
    public static final String DESTINATION = "Destination";
    public static final String TRANSPORT = "Transport";
    public static final String TOURIST_SPOT = "Tourist Spot";
    public static final String TRADITIONAL_FOOD = "Traditional Food";
    public static final String TRADITIONAL_ITEM = "Traditional Item";
    public static final String MARKET = "Market";
    public static final String ROUTE = "Route";
    public static final String CHAT_HISTORY = "Chat History";

    // Hotel Status
    public static final String HOTEL_STATUS_ACTIVE = "ACTIVE";
    public static final String HOTEL_STATUS_INACTIVE = "INACTIVE";
    public static final String HOTEL_STATUS_MAINTENANCE = "MAINTENANCE";

    // Room Status
    public static final String ROOM_STATUS_AVAILABLE = "AVAILABLE";
    public static final String ROOM_STATUS_BOOKED = "BOOKED";
    public static final String ROOM_STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String ROOM_STATUS_UNAVAILABLE = "UNAVAILABLE";
}

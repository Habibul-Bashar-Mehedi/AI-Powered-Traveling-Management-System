package aptms.enums;

public enum RoomStatus {
    AVAILABLE("AVAILABLE"),
    BOOKED("BOOKED"),
    MAINTENANCE("MAINTENANCE"),
    UNAVAILABLE("UNAVAILABLE");

    private final String value;

    RoomStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static RoomStatus fromValue(String value) {
        for (RoomStatus status : RoomStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid room status: " + value);
    }
}

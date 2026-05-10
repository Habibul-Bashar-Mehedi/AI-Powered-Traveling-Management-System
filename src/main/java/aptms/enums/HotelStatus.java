package aptms.enums;

public enum HotelStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    MAINTENANCE("MAINTENANCE");

    private final String value;

    HotelStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static HotelStatus fromValue(String value) {
        for (HotelStatus status : HotelStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid hotel status: " + value);
    }
}

package aptms.enums;

import java.util.EnumMap;
import java.util.Map;

public enum VendorType {
    HOTEL,
    TOUR_GUIDE,
    TRANSPORT,
    TOURIST_SPOT,
    TRADITIONAL_FOOD,
    TRADITIONAL_ITEM,
    MARKET,
    DESTINATION,
    TRAVEL_PACKAGE;

    private static final Map<VendorType, ServiceType> TO_SERVICE_TYPE = new EnumMap<>(VendorType.class);

    static {
        TO_SERVICE_TYPE.put(HOTEL, ServiceType.HOTEL_ROOM);
        TO_SERVICE_TYPE.put(TOUR_GUIDE, ServiceType.TOUR_PACKAGE);
        TO_SERVICE_TYPE.put(TRANSPORT, ServiceType.TRANSPORT_ROUTE);
        TO_SERVICE_TYPE.put(TOURIST_SPOT, ServiceType.TOURIST_SPOT);
        TO_SERVICE_TYPE.put(TRADITIONAL_FOOD, ServiceType.TRADITIONAL_FOOD);
        TO_SERVICE_TYPE.put(TRADITIONAL_ITEM, ServiceType.TRADITIONAL_ITEM);
        TO_SERVICE_TYPE.put(MARKET, ServiceType.MARKET);
        TO_SERVICE_TYPE.put(DESTINATION, ServiceType.DESTINATION);
        TO_SERVICE_TYPE.put(TRAVEL_PACKAGE, ServiceType.TRAVEL_PACKAGE);
    }

    public ServiceType toServiceType() {
        ServiceType result = TO_SERVICE_TYPE.get(this);
        if (result == null) {
            throw new IllegalStateException("No ServiceType mapping for VendorType: " + this);
        }
        return result;
    }
}


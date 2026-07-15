package aptms.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /** Great-circle distance between two lat/lng points, in kilometers (haversine formula). */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Filters items down to those whose destination id (per destinationIdExtractor) appears in
     * orderedDestinationIds, and sorts them to match that same distance-ascending order — shared
     * by every content service's "nearby" filter so the ranking logic isn't duplicated per entity.
     */
    public static <T> List<T> orderByDestinationRank(
            List<T> items, Function<T, Long> destinationIdExtractor, List<Long> orderedDestinationIds) {
        Map<Long, Integer> rank = new HashMap<>();
        for (int i = 0; i < orderedDestinationIds.size(); i++) {
            rank.put(orderedDestinationIds.get(i), i);
        }
        return items.stream()
                .filter(item -> rank.containsKey(destinationIdExtractor.apply(item)))
                .sorted(Comparator.comparingInt(item -> rank.get(destinationIdExtractor.apply(item))))
                .collect(Collectors.toList());
    }
}

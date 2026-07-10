package aptms.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Remaining bookable capacity for a service on a given date (or date range),
 * so the traveler sees "X left" before submitting a booking.
 */
@Data
@AllArgsConstructor
public class ServiceAvailabilityDTO {
    private int maxCapacity;
    private int alreadyBooked;
    private int available;
}

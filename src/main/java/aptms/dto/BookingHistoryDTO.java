package aptms.dto;

import aptms.enums.BookingSource;
import aptms.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Unified view row merging direct hotel {@code Booking} and generic
 * {@code VendorBooking} records into one history feed.
 */
@Data
public class BookingHistoryDTO {
    private String id;
    private BookingSource source;
    private ServiceType serviceType;
    private String title;
    private Instant bookingDate;
    private LocalDate travelDate;
    private String status;
    private BigDecimal amount;

    /** Null when the underlying hotel/service has no Destination linked yet. */
    private Long destinationId;
    private String destinationName;
}

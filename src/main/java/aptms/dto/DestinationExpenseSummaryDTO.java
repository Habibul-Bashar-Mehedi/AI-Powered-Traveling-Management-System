package aptms.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A user's total spend and booking activity grouped by {@code Destination}.
 * {@code destinationId} is null for the synthetic "Other / Unspecified" bucket,
 * which collects bookings whose hotel/service has no Destination linked.
 */
@Data
public class DestinationExpenseSummaryDTO {
    private Long destinationId;
    private String destinationName;
    private BigDecimal totalSpent;
    private int bookingCount;
    private LocalDate firstTravelDate;
    private LocalDate lastTravelDate;
}

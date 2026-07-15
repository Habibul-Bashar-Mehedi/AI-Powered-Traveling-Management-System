package aptms.services;

import aptms.dto.BookingHistoryDTO;
import aptms.dto.DestinationExpenseSummaryDTO;

import java.util.List;
import java.util.UUID;

/**
 * Groups a user's unified booking history (see {@link BookingHistoryService}) by
 * Destination, for a "My Spending" view. Cancelled/rejected bookings never count
 * toward spend.
 */
public interface UserExpenseService {

    /**
     * @param sort "totalSpent" (default, highest spend first) or "latest" (most
     *             recently visited destination first)
     */
    List<DestinationExpenseSummaryDTO> getExpenseSummary(UUID userId, String sort);

    /**
     * @param destinationKey a Destination id, or the literal "other" for the
     *                       synthetic bucket of bookings with no linked Destination
     */
    List<BookingHistoryDTO> getExpenseDrilldown(UUID userId, String destinationKey);
}

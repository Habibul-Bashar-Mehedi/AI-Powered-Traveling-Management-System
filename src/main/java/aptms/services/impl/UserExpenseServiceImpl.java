package aptms.services.impl;

import aptms.dto.BookingHistoryDTO;
import aptms.dto.DestinationExpenseSummaryDTO;
import aptms.services.BookingHistoryService;
import aptms.services.UserExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserExpenseServiceImpl implements UserExpenseService {

    private static final String OTHER_BUCKET_KEY = "other";
    private static final String OTHER_BUCKET_NAME = "Other / Unspecified";
    /** Destination ids are positive auto-increment values, so this can never collide with a real one. */
    private static final Long NO_DESTINATION_KEY = -1L;

    /** Cancelled/rejected bookings were never actually paid for and don't count as spend. */
    private static final Set<String> EXCLUDED_STATUSES = Set.of("CANCELLED", "REJECTED");

    private final BookingHistoryService bookingHistoryService;

    @Override
    public List<DestinationExpenseSummaryDTO> getExpenseSummary(UUID userId, String sort) {
        List<BookingHistoryDTO> countedBookings = countedBookings(userId);

        var grouped = countedBookings.stream()
                .collect(Collectors.groupingBy(dto -> dto.getDestinationId() == null ? NO_DESTINATION_KEY : dto.getDestinationId()));

        List<DestinationExpenseSummaryDTO> summaries = grouped.entrySet().stream()
                .map(entry -> toSummary(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        Comparator<DestinationExpenseSummaryDTO> comparator = "latest".equalsIgnoreCase(sort)
                ? Comparator.comparing(DestinationExpenseSummaryDTO::getLastTravelDate).reversed()
                : Comparator.comparing(DestinationExpenseSummaryDTO::getTotalSpent).reversed();

        return summaries.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<BookingHistoryDTO> getExpenseDrilldown(UUID userId, String destinationKey) {
        List<BookingHistoryDTO> countedBookings = countedBookings(userId);

        if (OTHER_BUCKET_KEY.equalsIgnoreCase(destinationKey)) {
            return countedBookings.stream()
                    .filter(dto -> dto.getDestinationId() == null)
                    .collect(Collectors.toList());
        }

        Long destinationId;
        try {
            destinationId = Long.valueOf(destinationKey);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid destination id: " + destinationKey);
        }

        return countedBookings.stream()
                .filter(dto -> destinationId.equals(dto.getDestinationId()))
                .collect(Collectors.toList());
    }

    private List<BookingHistoryDTO> countedBookings(UUID userId) {
        return bookingHistoryService.getAllHistory(userId).stream()
                .filter(dto -> !EXCLUDED_STATUSES.contains(dto.getStatus()))
                .collect(Collectors.toList());
    }

    private DestinationExpenseSummaryDTO toSummary(Long destinationId, List<BookingHistoryDTO> bookings) {
        boolean isOtherBucket = NO_DESTINATION_KEY.equals(destinationId);
        DestinationExpenseSummaryDTO summary = new DestinationExpenseSummaryDTO();
        summary.setDestinationId(isOtherBucket ? null : destinationId);
        summary.setDestinationName(isOtherBucket ? OTHER_BUCKET_NAME : bookings.get(0).getDestinationName());
        summary.setTotalSpent(bookings.stream().map(BookingHistoryDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setBookingCount(bookings.size());
        summary.setFirstTravelDate(bookings.stream().map(BookingHistoryDTO::getTravelDate).min(Comparator.naturalOrder()).orElse(null));
        summary.setLastTravelDate(bookings.stream().map(BookingHistoryDTO::getTravelDate).max(Comparator.naturalOrder()).orElse(null));
        return summary;
    }
}

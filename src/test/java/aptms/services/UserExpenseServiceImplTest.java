package aptms.services;

import aptms.dto.BookingHistoryDTO;
import aptms.dto.DestinationExpenseSummaryDTO;
import aptms.enums.BookingSource;
import aptms.enums.ServiceType;
import aptms.services.impl.UserExpenseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserExpenseServiceImplTest {

    @Mock
    private BookingHistoryService bookingHistoryService;

    @InjectMocks
    private UserExpenseServiceImpl userExpenseService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private BookingHistoryDTO booking(String id, Long destId, String destName, String status, String amount, String travelDate) {
        BookingHistoryDTO dto = new BookingHistoryDTO();
        dto.setId(id);
        dto.setSource(BookingSource.VENDOR_SERVICE);
        dto.setServiceType(ServiceType.TOUR_PACKAGE);
        dto.setTitle("Trip " + id);
        dto.setStatus(status);
        dto.setAmount(new BigDecimal(amount));
        dto.setTravelDate(LocalDate.parse(travelDate));
        dto.setDestinationId(destId);
        dto.setDestinationName(destName);
        return dto;
    }

    @Test
    void groupsByDestinationAndSumsSpendExcludingCancelledAndRejected() {
        when(bookingHistoryService.getAllHistory(userId)).thenReturn(List.of(
                booking("1", 1L, "Cox's Bazar", "CONFIRMED", "100.00", "2026-01-10"),
                booking("2", 1L, "Cox's Bazar", "COMPLETED", "200.00", "2026-02-15"),
                booking("3", 1L, "Cox's Bazar", "CANCELLED", "9999.00", "2026-03-01"),
                booking("4", 2L, "Sylhet", "REJECTED", "500.00", "2026-01-01"),
                booking("5", null, null, "CONFIRMED", "50.00", "2026-01-05")
        ));

        List<DestinationExpenseSummaryDTO> result = userExpenseService.getExpenseSummary(userId, "totalSpent");

        assertEquals(2, result.size());

        DestinationExpenseSummaryDTO coxsBazar = result.stream().filter(r -> r.getDestinationId() != null).findFirst().orElseThrow();
        assertEquals("Cox's Bazar", coxsBazar.getDestinationName());
        assertEquals(0, new BigDecimal("300.00").compareTo(coxsBazar.getTotalSpent()));
        assertEquals(2, coxsBazar.getBookingCount());
        assertEquals(LocalDate.parse("2026-01-10"), coxsBazar.getFirstTravelDate());
        assertEquals(LocalDate.parse("2026-02-15"), coxsBazar.getLastTravelDate());

        DestinationExpenseSummaryDTO other = result.stream().filter(r -> r.getDestinationId() == null).findFirst().orElseThrow();
        assertEquals("Other / Unspecified", other.getDestinationName());
        assertEquals(0, new BigDecimal("50.00").compareTo(other.getTotalSpent()));
        assertEquals(1, other.getBookingCount());
    }

    @Test
    void sortsByTotalSpentDescByDefault() {
        when(bookingHistoryService.getAllHistory(userId)).thenReturn(List.of(
                booking("1", 1L, "Cheap Town", "CONFIRMED", "50.00", "2026-01-01"),
                booking("2", 2L, "Expensive City", "CONFIRMED", "500.00", "2026-01-02")
        ));

        List<DestinationExpenseSummaryDTO> result = userExpenseService.getExpenseSummary(userId, null);

        assertEquals("Expensive City", result.get(0).getDestinationName());
        assertEquals("Cheap Town", result.get(1).getDestinationName());
    }

    @Test
    void sortsByLatestVisitWhenRequested() {
        when(bookingHistoryService.getAllHistory(userId)).thenReturn(List.of(
                booking("1", 1L, "Visited Long Ago", "CONFIRMED", "500.00", "2020-01-01"),
                booking("2", 2L, "Visited Recently", "CONFIRMED", "50.00", "2026-06-01")
        ));

        List<DestinationExpenseSummaryDTO> result = userExpenseService.getExpenseSummary(userId, "latest");

        assertEquals("Visited Recently", result.get(0).getDestinationName());
        assertEquals("Visited Long Ago", result.get(1).getDestinationName());
    }

    @Test
    void drilldownReturnsOnlyBookingsForThatDestination() {
        when(bookingHistoryService.getAllHistory(userId)).thenReturn(List.of(
                booking("1", 1L, "Cox's Bazar", "CONFIRMED", "100.00", "2026-01-10"),
                booking("2", 2L, "Sylhet", "CONFIRMED", "500.00", "2026-01-01")
        ));

        List<BookingHistoryDTO> result = userExpenseService.getExpenseDrilldown(userId, "1");

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    void drilldownOtherReturnsOnlyUnlinkedBookings() {
        when(bookingHistoryService.getAllHistory(userId)).thenReturn(List.of(
                booking("1", 1L, "Cox's Bazar", "CONFIRMED", "100.00", "2026-01-10"),
                booking("2", null, null, "CONFIRMED", "50.00", "2026-01-01")
        ));

        List<BookingHistoryDTO> result = userExpenseService.getExpenseDrilldown(userId, "other");

        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getId());
    }
}

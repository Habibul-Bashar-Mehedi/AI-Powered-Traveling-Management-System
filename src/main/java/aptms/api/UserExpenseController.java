package aptms.api;

import aptms.dto.BookingHistoryDTO;
import aptms.dto.DestinationExpenseSummaryDTO;
import aptms.security.SecurityUtils;
import aptms.services.UserExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * "My Spending" view for the authenticated user: their unified booking history
 * grouped by Destination.
 */
@RestController
@RequestMapping("/api/user/expenses")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "User Expenses", description = "Spending summary grouped by destination")
public class UserExpenseController {

    private final UserExpenseService userExpenseService;

    @GetMapping("/summary")
    @Operation(summary = "Get the current user's spend, booking count, and travel date range per destination")
    public ResponseEntity<List<DestinationExpenseSummaryDTO>> getSummary(
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(userExpenseService.getExpenseSummary(SecurityUtils.getCurrentUserId(), sort));
    }

    @GetMapping("/{destinationId}")
    @Operation(summary = "Drill down into every booking the user made for a given destination (or 'other')")
    public ResponseEntity<List<BookingHistoryDTO>> getDrilldown(@PathVariable String destinationId) {
        return ResponseEntity.ok(userExpenseService.getExpenseDrilldown(SecurityUtils.getCurrentUserId(), destinationId));
    }
}

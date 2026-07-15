package aptms.api;

import aptms.dto.BookingHistoryDTO;
import aptms.enums.BookingSource;
import aptms.security.SecurityUtils;
import aptms.services.BookingHistoryService;
import aptms.services.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified booking history for the authenticated user, merging direct hotel
 * bookings and generic vendor-service bookings into one feed.
 */
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Tag(name = "Booking History", description = "Unified booking history across hotel-direct and vendor-service bookings")
public class BookingHistoryController {

    private final BookingHistoryService bookingHistoryService;
    private final ReceiptService receiptService;

    @GetMapping("/history")
    @Operation(summary = "Get the current user's merged booking history, optionally filtered by type/status")
    public ResponseEntity<Page<BookingHistoryDTO>> getHistory(
            @RequestParam(required = false) BookingSource type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(bookingHistoryService.getHistory(SecurityUtils.getCurrentUserId(), type, status, pageable));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Download a PDF receipt for one of the current user's bookings (hotel-direct or vendor-service)")
    public ResponseEntity<byte[]> getReceipt(@PathVariable String id) {
        byte[] pdf = receiptService.generateReceipt(id, SecurityUtils.getCurrentUserId());
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("receipt-" + id + ".pdf")
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

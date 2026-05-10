package aptms.api;

import aptms.entities.Booking;
import aptms.enums.BookingStatus;
import aptms.services.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static aptms.constants.BookingConstants.*;
import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/booking")
public class BookingRestController {
    private final BookingService bookingService;

    public BookingRestController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/add")
    public ResponseEntity<Booking> postBooking(@RequestBody Booking booking) {
        Booking createdBooking = bookingService.booking(booking);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }

    @GetMapping()
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateBooking(@PathVariable long id, @RequestBody BookingUpdateRequest request) {
        boolean updated = bookingService.updateBooking(
            id,
            request.getCheckInDate(),
            request.getCheckOutDate(), 
            request.getGuestCount(), 
            request.getTotalPrice(),
            request.getStatus(), 
            request.getSpecialRequest()
        );

        if(updated) {
            return ResponseEntity.ok(BOOKING_UPDATED_MESSAGE);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BOOKING_NOT_FOUND_MESSAGE + id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBooking(@PathVariable long id) {
        String result = bookingService.deleteBooking(id);
        return ResponseEntity.ok(result);
    }
    
    // Inner class for update request
    public static class BookingUpdateRequest {
        private Date checkInDate;
        private Date checkOutDate;
        private int guestCount;
        private Double totalPrice;
        private BookingStatus status;
        private String specialRequest;
        
        // Getters and setters
        public Date getCheckInDate() { return checkInDate; }
        public void setCheckInDate(Date checkInDate) { this.checkInDate = checkInDate; }
        public Date getCheckOutDate() { return checkOutDate; }
        public void setCheckOutDate(Date checkOutDate) { this.checkOutDate = checkOutDate; }
        public int getGuestCount() { return guestCount; }
        public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
        public Double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
        public BookingStatus getStatus() { return status; }
        public void setStatus(BookingStatus status) { this.status = status; }
        public String getSpecialRequest() { return specialRequest; }
        public void setSpecialRequest(String specialRequest) { this.specialRequest = specialRequest; }
    }
}

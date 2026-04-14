package aptms.api;

import aptms.entities.Booking;
import aptms.services.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
public class BookingRestController {
    @Autowired
    private BookingService bookingService;

    @PostMapping("/add")
    public Booking postBooking(@RequestBody Booking booking) {
        return bookingService.booking(booking);
    }

    @GetMapping()
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateBooking(@PathVariable long id, @RequestBody Booking booking) {
        boolean update = bookingService.updateBooking(id,booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getGuestCount(), booking.getTotalPrice(),
                booking.getStatus(), booking.getSpecialRequest());

        if(update) {
            return ResponseEntity.ok("booking updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("booking not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBooking(@PathVariable long id) {
        String result = bookingService.deleteBooking(id);
        if(result.equals("booking is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}

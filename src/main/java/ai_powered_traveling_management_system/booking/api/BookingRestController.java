package ai_powered_traveling_management_system.booking.api;

import ai_powered_traveling_management_system.booking.entities.Booking;
import ai_powered_traveling_management_system.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/booking")
public class BookingRestController {
    @Autowired
    private BookingService bookingService;

    @PostMapping("/add")
    public String postBooking(@RequestBody Booking booking1) {
        return bookingService.booking(booking1);
    }

    @GetMapping("/all")
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }
}

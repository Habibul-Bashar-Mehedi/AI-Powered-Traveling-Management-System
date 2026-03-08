package ai_powered_traveling_management_system.booking.api;

import ai_powered_traveling_management_system.booking.entities.Booking;
import ai_powered_traveling_management_system.booking.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/booking")
public class BookingRestController {
    @Autowired
    private BookingService bookingService;

    @PostMapping("/add")
    public String postBooking(@RequestBody Booking booking1) {
        return bookingService.booking(booking1);
    }
}

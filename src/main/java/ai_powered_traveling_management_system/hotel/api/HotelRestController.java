package ai_powered_traveling_management_system.hotel.api;

import ai_powered_traveling_management_system.hotel.entities.Hotel;
import ai_powered_traveling_management_system.hotel.services.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelRestController {
    @Autowired
    private HotelService hotelService;

    @PostMapping("/add")
    public String addHotel(@RequestBody Hotel hotel) {
        return hotelService.addHotel(hotel);
    }
    @GetMapping("/all")
    public List<Hotel> getAllHotels() {
        return hotelService.getAllHotel();
    }
}

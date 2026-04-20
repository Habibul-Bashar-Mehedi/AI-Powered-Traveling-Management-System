package aptms.api;

import aptms.entities.Hotel;
import aptms.services.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelRestController {
    private final HotelService hotelService;

    public HotelRestController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping("/add")
    public Hotel addHotel(@RequestBody Hotel hotel) {
        return hotelService.addHotel(hotel);
    }

    @GetMapping()
    public List<Hotel> getAllHotels() {
        return hotelService.getAllHotel();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateHotel(@PathVariable long id, @RequestBody Hotel hotel) {
        boolean update = hotelService.updateHotel(id,hotel.getHotelName(),
                hotel.getAddress(), hotel.getStatus(), hotel.getDescriptions());

        if(update) {
            return ResponseEntity.ok("hotel updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("hotel not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHotel(@PathVariable long id) {
        String result = hotelService.deleteHotel(id);
        if(result.equals("hotel is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}

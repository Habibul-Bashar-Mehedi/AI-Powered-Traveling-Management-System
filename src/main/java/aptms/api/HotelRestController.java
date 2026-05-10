package aptms.api;

import aptms.constants.EntityConstants;
import aptms.entities.Hotel;
import aptms.enums.HotelStatus;
import aptms.services.HotelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelRestController {
    private final HotelService hotelService;

    public HotelRestController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping("/add")
    public ResponseEntity<Hotel> addHotel(@RequestBody Hotel hotel) {
        Hotel createdHotel = hotelService.addHotel(hotel);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdHotel);
    }

    @GetMapping()
    public ResponseEntity<List<Hotel>> getAllHotels() {
        List<Hotel> hotels = hotelService.getAllHotel();
        return ResponseEntity.ok(hotels);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateHotel(@PathVariable long id, @RequestBody HotelUpdateRequest request) {
        hotelService.updateHotel(id, request.hotelName, request.address, request.status, request.descriptions);
        return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, HOTEL));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHotel(@PathVariable long id) {
        String result = hotelService.deleteHotel(id);
        return ResponseEntity.ok(result);
    }

    // Inner class for update requests
    public static class HotelUpdateRequest {
        public String hotelName;
        public String address;
        public HotelStatus status;
        public String descriptions;
    }
}

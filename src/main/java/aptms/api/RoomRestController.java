package aptms.api;

import aptms.entities.Room;
import aptms.enums.RoomStatus;
import aptms.services.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static aptms.constants.EntityConstants.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {
    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/add")
    public ResponseEntity<Room> postRoom(@RequestBody Room room) {
        Room created = roomService.addRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping()
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomService.getAllRoom();
        return ResponseEntity.ok(rooms);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateRoom(@PathVariable long id, @RequestBody RoomUpdateRequest request) {
        boolean updated = roomService.updateRoom(
            id,
            request.getRoomTypeName(),
            request.getAmenities(),
            request.getPricePerNight(),
            request.getAvailableQuantities(),
            request.getStatus()
        );

        if(updated) {
            return ResponseEntity.ok(String.format(ENTITY_UPDATED_MESSAGE, ROOM));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(String.format(ENTITY_NOT_FOUND_MESSAGE, ROOM, id));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoom(@PathVariable long id) {
        String result = roomService.deleteRoom(id);
        return ResponseEntity.ok(result);
    }
    
    // Inner class for update request
    public static class RoomUpdateRequest {
        private String roomTypeName;
        private String amenities;
        private double pricePerNight;
        private int availableQuantities;
        private RoomStatus status;
        
        // Getters and setters
        public String getRoomTypeName() { return roomTypeName; }
        public void setRoomTypeName(String roomTypeName) { this.roomTypeName = roomTypeName; }
        public String getAmenities() { return amenities; }
        public void setAmenities(String amenities) { this.amenities = amenities; }
        public double getPricePerNight() { return pricePerNight; }
        public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
        public int getAvailableQuantities() { return availableQuantities; }
        public void setAvailableQuantities(int availableQuantities) { this.availableQuantities = availableQuantities; }
        public RoomStatus getStatus() { return status; }
        public void setStatus(RoomStatus status) { this.status = status; }
    }
}

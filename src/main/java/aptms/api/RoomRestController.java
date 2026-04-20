package aptms.api;

import aptms.entities.Room;
import aptms.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {
    private final RoomService roomService;

    public RoomRestController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/add")
    public Room postRoom(@RequestBody Room room) {
        return roomService.addRoom(room);
    }

    @GetMapping()
    public List<Room> getAllRooms () {
        return roomService.getAllRoom();
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateRoom(@PathVariable long id, @RequestBody Room room) {
        boolean update = roomService.updateRoom(id,room.getRoomTypeName(),
                room.getAmenities(),
                room.getPricePerNight(),
                room.getAvailableQuantities(),
                room.getStatus());

        if(update) {
            return ResponseEntity.ok("room updated successfully");
        }else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("room not found with id: "+id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoom(@PathVariable long id) {
        String result = roomService.deleteRoom(id);
        if(result.equals("room is deleted")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}

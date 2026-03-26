package aptms.api;

import aptms.entities.Room;
import aptms.services.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomRestController {
    @Autowired
    private RoomService roomService;

    @PostMapping("/add")
    public String postRoom(@RequestBody Room room) {
        return roomService.addRoom(room);
    }

    @GetMapping("/all")
    public List<Room> getAllRooms () {
        return roomService.getAllRoom();
    }
}

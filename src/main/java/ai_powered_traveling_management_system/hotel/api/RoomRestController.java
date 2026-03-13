package ai_powered_traveling_management_system.hotel.api;

import ai_powered_traveling_management_system.hotel.entities.Room;
import ai_powered_traveling_management_system.hotel.services.RoomService;
import org.hibernate.boot.internal.Abstract;
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

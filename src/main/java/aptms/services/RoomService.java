package aptms.services;

import aptms.entities.Room;
import aptms.repositories.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {
    @Autowired
    private RoomRepository roomRepository;

    public String addRoom(Room room) {
        // Validation
        if (room.getHotel() == null || room.getHotel().getId() == 0) {
            throw new RuntimeException("Error: Hotel ID is missing!");
        }

        // existsBy logic
        boolean exists = roomRepository.existsByRoomTypeNameAndHotelId(
                room.getRoomTypeName(),
                room.getHotel().getId()
        );

        if (exists) {
            throw new RuntimeException("This room type already exists for this hotel.");
        }

        roomRepository.save(room);
        return "Room Successfully added";
    }

    public List<Room> getAllRoom() {
        return roomRepository.findAll();
    }
}

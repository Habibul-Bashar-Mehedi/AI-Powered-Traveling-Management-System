package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Room;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Room addRoom(Room room) {
        // Validation
        if (room.getHotel() == null || room.getHotel().getId() == 0) {
            throw new InvalidException("Error: Hotel ID is missing!");
        }

        // existsBy logic
        boolean exists = roomRepository.existsByRoomTypeNameAndHotelId(
                room.getRoomTypeName(),
                room.getHotel().getId()
        );

        if (exists) {
            throw new DuplicateValueFoundExceptions("This room type already exists for this hotel.");
        }

        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Room> getAllRoom() {
        return roomRepository.findAll();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteRoom(long id) {
        if(!roomRepository.existsById(id)) throw new IdNotFoundException("room id not found");


        roomRepository.deleteById(id);
        return "room is deleted";
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateRoom(long id,String roomTypeName,
                              String amenities,double pricePerNight,
                              int availableQuantities,String status) {

        return roomRepository.findById(id).map(room -> {
            room.setRoomTypeName(roomTypeName);
            room.setAmenities(amenities);
            room.setPricePerNight(pricePerNight);
            room.setAvailableQuantities(availableQuantities);
            room.setStatus(status);

            roomRepository.save(room);
            return true;
        }).orElseThrow(()->
                new IdNotFoundException("room id not found")
        );

    }
}

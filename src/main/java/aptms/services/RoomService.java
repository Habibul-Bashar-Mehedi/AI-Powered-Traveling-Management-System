package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Room;
import aptms.enums.RoomStatus;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RoomRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static aptms.constants.EntityConstants.*;
import static aptms.constants.ValidationConstants.*;

@Service
public class RoomService {

    private static final int MAX_LIST_SIZE = 500;

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Room addRoom(Room room) {
        // Validation
        if (room.getHotel() == null || room.getHotel().getId() == null) {
            throw new InvalidException(
                String.format(REQUIRED_FIELD_MESSAGE, "Hotel")
            );
        }

        // Check if room type already exists for this hotel
        boolean exists = roomRepository.existsByRoomTypeNameAndHotelId(
                room.getRoomTypeName(),
                room.getHotel().getId()
        );

        if (exists) {
            throw new DuplicateValueFoundExceptions(
                String.format(DUPLICATE_ENTRY_MESSAGE, ROOM)
            );
        }
        
        // Set default status if not provided
        if(room.getStatus() == null) {
            room.setStatus(RoomStatus.AVAILABLE);
        }

        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Room> getAllRoom() {
        return roomRepository.findAll(PageRequest.of(0, MAX_LIST_SIZE)).getContent();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteRoom(long id) {
        if(!roomRepository.existsById(id)) {
            throw new IdNotFoundException(
                String.format(ENTITY_NOT_FOUND_MESSAGE, ROOM, id)
            );
        }

        roomRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, ROOM);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateRoom(long id, String roomTypeName,
                              String amenities, double pricePerNight,
                              int availableQuantities, RoomStatus status) {

        return roomRepository.findById(id).map(room -> {
            room.setRoomTypeName(roomTypeName);
            room.setAmenities(amenities);
            room.setPricePerNight(pricePerNight);
            room.setAvailableQuantities(availableQuantities);
            room.setStatus(status);

            roomRepository.save(room);
            return true;
        }).orElseThrow(() ->
                new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, ROOM, id)
                )
        );
    }
}

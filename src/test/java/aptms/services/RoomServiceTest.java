package aptms.services;

import aptms.entities.Hotel;
import aptms.entities.Room;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(10L);

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setHotel(testHotel);
        testRoom.setRoomTypeName("VIP 1 master bed");
        testRoom.setAmenities("tv,ac,washroom");
        testRoom.setStatus("Available");
        testRoom.setPricePerNight(5000.0);
        testRoom.setAvailableQuantities(10);
    }


    @Test
    void addRoomTest() {
        when(roomRepository.existsByRoomTypeNameAndHotelId(anyString(), anyLong())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        Room result = roomService.addRoom(testRoom);

        assertNotNull(result);
        assertEquals("VIP 1 master bed", result.getRoomTypeName());
        verify(roomRepository, times(1)).save(testRoom);
    }

    @Test
    void missingRoomTest() {
        testRoom.setHotel(null);
        assertThrows(InvalidException.class, () -> roomService.addRoom(testRoom));
    }

    @Test
    void alreadyExistsTest() {
        when(roomRepository.existsByRoomTypeNameAndHotelId(anyString(), anyLong())).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> roomService.addRoom(testRoom));
    }

    @Test
    void getAllRoomTest() {
        when(roomRepository.findAll()).thenReturn(List.of(testRoom));
        List<Room> rooms = roomService.getAllRoom();
        assertEquals(1, rooms.size());
    }


    @Test
    void deleteRoomTest() {
        when(roomRepository.existsById(1L)).thenReturn(true);

        String response = roomService.deleteRoom(1L);

        assertEquals("room is deleted", response);
        verify(roomRepository, times(1)).deleteById(1L);
    }

    @Test
    void updateRoomTest() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        boolean result = roomService.updateRoom(1L, "Deluxe", "wifi", 6000.0, 5, "Booked");

        assertTrue(result);
        assertEquals("Deluxe", testRoom.getRoomTypeName());
        verify(roomRepository, times(1)).save(testRoom);
    }

}
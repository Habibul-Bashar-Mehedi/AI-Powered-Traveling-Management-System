package ai_powered_traveling_management_system.hotel.repositories;

import ai_powered_traveling_management_system.hotel.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // Ek-i hotel-e ek-i type-er room (jemon Deluxe) bar bar add hobe na
    boolean existsByRoomTypeNameAndHotelId(String roomTypeName, Long hotelId);
}

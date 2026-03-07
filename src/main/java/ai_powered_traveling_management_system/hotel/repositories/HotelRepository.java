package ai_powered_traveling_management_system.hotel.repositories;

import ai_powered_traveling_management_system.hotel.entities.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsHotelByHotelNameAndAddress(String hotelName, String address);
}

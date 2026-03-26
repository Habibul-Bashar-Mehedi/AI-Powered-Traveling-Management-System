package aptms.repositories;

import aptms.entities.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    boolean existsHotelByHotelNameAndAddress(String hotelName, String address);
}

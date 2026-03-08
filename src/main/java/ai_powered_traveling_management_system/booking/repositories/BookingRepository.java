package ai_powered_traveling_management_system.booking.repositories;

import ai_powered_traveling_management_system.booking.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId " +
            "AND b.status != 'CANCELLED' " +
            "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean isRoomBooked(@Param("roomId") Long roomId,
                         @Param("checkIn") Date checkIn,
                         @Param("checkOut") Date checkOut);
}
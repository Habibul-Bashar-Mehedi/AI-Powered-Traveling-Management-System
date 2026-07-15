package aptms.repositories;

import aptms.entities.Booking;
import aptms.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.room.id = :roomId " +
            "AND b.status != :cancelledStatus " +
            "AND (:checkIn < b.checkOutDate AND :checkOut > b.checkInDate)")
    boolean isRoomBooked(@Param("roomId") Long roomId,
                         @Param("checkIn") Date checkIn,
                         @Param("checkOut") Date checkOut,
                         @Param("cancelledStatus") BookingStatus cancelledStatus);

    /** User dashboard: hotel + room fetched eagerly for booking history display. */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.hotel h
        JOIN FETCH b.room r
        LEFT JOIN FETCH h.destination
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") UUID userId);

    /** Ownership-scoped lookup for a single booking (e.g. receipt generation) — never trust a client-supplied id alone. */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.hotel h
        JOIN FETCH b.room r
        JOIN FETCH b.user u
        LEFT JOIN FETCH h.destination
        WHERE b.id = :id AND b.user.id = :userId
    """)
    Optional<Booking> findByIdAndUserId(@Param("id") Long id, @Param("userId") UUID userId);
}
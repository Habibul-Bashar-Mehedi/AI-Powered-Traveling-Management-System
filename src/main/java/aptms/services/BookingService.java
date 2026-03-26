package aptms.services;

import aptms.entities.Booking;
import aptms.repositories.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    public String booking(Booking booking) {
        if(booking.getRoom() == null || booking.getHotel() == null || booking.getUser() == null) {
            throw new RuntimeException("User, Room, and Hotel information are required!");
        }

        // Logic Fix: Date overlap check korun
        boolean isAlreadyBooked = bookingRepository.isRoomBooked(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        if(isAlreadyBooked){
            throw new RuntimeException("This room is already booked for the selected dates!");
        }

        bookingRepository.save(booking);
        return "Booking successfully done";
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}

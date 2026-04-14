package aptms.services;

import aptms.entities.Booking;
import aptms.repositories.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;
    public Booking booking(Booking booking) {
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

        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    //booking
    public String deleteBooking(long id) {
        if(!bookingRepository.existsById(id)) return "booking not found";

        bookingRepository.deleteById(id);
        return "booking is deleted";
    }

    public boolean updateBooking(long id, Date checkInDate,
                                 Date checkOutDate,int guestCount,
                                 Double totalPrice,String status,
                                 String specialRequest) {

        return bookingRepository.findById(id).map(booking -> {
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);
            booking.setGuestCount(guestCount);
            booking.setTotalPrice(totalPrice);
            booking.setStatus(status);
            booking.setSpecialRequest(specialRequest);

            bookingRepository.save(booking);
            return true;
        }).orElse(false);

    }
}

package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Booking;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.BookingRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BookingService {
    
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Booking booking(Booking booking) {
        if(booking.getRoom() == null || booking.getHotel() == null || booking.getUser() == null) {
            throw new InvalidException("User, Room, and Hotel information are required!");
        }

        // Logic Fix: Date overlap check korun
        boolean isAlreadyBooked = bookingRepository.isRoomBooked(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        if(isAlreadyBooked){
            throw new DuplicateValueFoundExceptions("This room is already booked for the selected dates!");
        }

        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }


    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteBooking(long id) {
        if(!bookingRepository.existsById(id)) throw new IdNotFoundException("booking id not found");

        bookingRepository.deleteById(id);
        return "booking is deleted";
    }

    @Transactional
    @SecureAction(role = "ADMIN")
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
        }).orElseThrow(()->
                new IdNotFoundException("booking id not found")
        );

    }
}

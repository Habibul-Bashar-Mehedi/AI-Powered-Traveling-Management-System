package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Booking;
import aptms.enums.BookingStatus;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static aptms.constants.BookingConstants.*;
import static aptms.constants.EntityConstants.*;

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
            throw new InvalidException(BOOKING_VALIDATION_ERROR);
        }

        boolean isAlreadyBooked = bookingRepository.isRoomBooked(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                BookingStatus.CANCELLED
        );

        if(isAlreadyBooked){
            throw new DuplicateValueFoundExceptions(ROOM_ALREADY_BOOKED_MESSAGE);
        }
        
        // Set default status if not provided
        if(booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
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
        if(!bookingRepository.existsById(id)) {
            throw new IdNotFoundException(
                String.format(ENTITY_NOT_FOUND_MESSAGE, BOOKING, id)
            );
        }

        bookingRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, BOOKING);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateBooking(long id, Date checkInDate,
                                 Date checkOutDate, int guestCount,
                                 Double totalPrice, BookingStatus status,
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
        }).orElseThrow(() ->
                new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, BOOKING, id)
                )
        );
    }
}

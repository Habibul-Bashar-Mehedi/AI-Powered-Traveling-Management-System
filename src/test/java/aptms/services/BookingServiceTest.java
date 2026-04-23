package aptms.services;

import aptms.entities.*;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;
    private User testUser;
    private Room testRoom;
    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);

        testHotel = new Hotel();
        testHotel.setId(20L);

        testRoom = new Room();
        testRoom.setId(30L);

        testBooking = new Booking();
        testBooking.setUser(testUser);
        testBooking.setRoom(testRoom);
        testBooking.setHotel(testHotel);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        testBooking.setCheckInDate(new Date());
        testBooking.setCheckOutDate(cal.getTime());

        testBooking.setGuestCount(2);
        testBooking.setTotalPrice(5000.00);
        testBooking.setStatus("PENDING");

    }

    @Test
    void testBookingSuccess() {
        when(bookingRepository.isRoomBooked(eq(30L),any(Date.class),any(Date.class))).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

        Booking result = bookingService.booking(testBooking);

        assertThat(result).isNotNull();
        assertThat(result.getRoom().getId()).isEqualTo(30L);
        assertEquals("PENDING",result.getStatus());
        verify(bookingRepository,times(1)).save(testBooking);
    }

    @Test
    void alreadyBookedTest() {
        when(bookingRepository.isRoomBooked(eq(30L),any(Date.class),any(Date.class))).thenReturn(true);

        assertThrows(DuplicateValueFoundExceptions.class,()-> {
            bookingService.booking(testBooking);
        });

        verify(bookingRepository,never()).save(any(Booking.class));


    }

    @Test
    void missingRoomTest() {
        testBooking.setRoom(null);

        assertThrows(InvalidException.class,()->{
            bookingService.booking(testBooking);
        });
    }

    @Test
    void deleteTest() {
        long id = 1L;

        when(bookingRepository.existsById(id)).thenReturn(true);

        String response = bookingService.deleteBooking(id);

        assertEquals("booking is deleted",response);
        verify(bookingRepository,times(1)).deleteById(id);
    }


    @Test
    void updateBooking_Success() {
        // Arrange
        long id = 1L;
        Date newIn = new Date();
        Date newOut = new Date(System.currentTimeMillis() + 172800000); // +2 days

        when(bookingRepository.findById(id)).thenReturn(Optional.of(testBooking));

        // Act
        boolean result = bookingService.updateBooking(id, newIn, newOut, 2, 500.0, "CONFIRMED", "None");

        // Assert
        assertThat(result).isTrue();
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void getAllBookingTest() {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));
        List<Booking> bookings = bookingService.getAllBookings();
        assertEquals(1, bookings.size());
    }
}

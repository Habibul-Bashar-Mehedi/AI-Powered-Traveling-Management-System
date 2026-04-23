package aptms.services;

import aptms.entities.Destination;
import aptms.entities.Hotel;
import aptms.entities.Transport;
import aptms.entities.User;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HotelServiceTest {
    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    private User vendor;
    private Hotel hotel;
    private Destination destination;

    @BeforeEach
    void setUp() {
        vendor = new User();
        vendor.setId(10L);

        destination = new Destination();
        destination.setId(20L);

        hotel = new Hotel();
        hotel.setVendor(vendor);
        hotel.setDestinationId(destination);
        hotel.setHotelName("shunar-bangla-hotel");
        hotel.setAddress("hotel");
        hotel.setStatus("VIP");
        hotel.setDescriptions("Experience world-class service at Hotel Sonar Bangla, featuring sea-view rooms and elite dining.");

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        hotel.setCreatedAt(calendar.getTime());
        hotel.setUpdatedAt(calendar.getTime());
        hotel.setDeleted(false);

    }

    @Test
    void addHotelTest() {
        when(hotelRepository
                .existsHotelByHotelNameAndAddress(
                        hotel.getHotelName(),hotel.getAddress())
        ).thenReturn(false);
        when(hotelRepository.save(any(Hotel.class))).thenReturn(hotel);

        Hotel result = hotelService.addHotel(hotel);

        assertThat(result).isNotNull();
        assertThat(result.getDestinationId().getId()).isEqualTo(20L);
        assertThat(result.getVendor().getId()).isEqualTo(10L);
        assertEquals("VIP",result.getStatus());
        verify(hotelRepository,times(1)).save(hotel);
    }

    @Test
    void alreadyExist() {
        when(hotelRepository
                .existsHotelByHotelNameAndAddress(
                        hotel.getHotelName(),hotel.getAddress())
        ).thenReturn(true);

        assertThrows(DuplicateValueFoundExceptions.class,()->{
            hotelService.addHotel(hotel);
        });

        verify(hotelRepository,never()).save(hotel);
    }

    @Test
    void missingHotelTest() {
        hotel.setHotelName(null);

        assertThrows(InvalidException.class,()->{
            hotelService.addHotel(hotel);
        });

    }

    @Test
    void deleteTest() {
        long id = 1L;

        when(hotelRepository.existsById(id)).thenReturn(true);
        String response = hotelService.deleteHotel(id);
        assertEquals("Hotel has been successfully deleted.",response);

        verify(hotelRepository,times(1)).deleteById(id);
    }

    @Test
    void updateTest() {
        long id = 1L;
        String newName = "new hotel";
        String newAddress = "savar";

        when(hotelRepository.existsHotelByHotelNameAndAddress(
                eq(newName),
                eq(newAddress))
        ).thenReturn(false);
        when(hotelRepository.findById(id)).thenReturn(java.util.Optional.of(hotel));

        boolean isUpdated = hotelService.updateHotel(
                id,
                newName,
                newAddress,
                "5star",
                "beautiful hotel with swimming pool"
        );
        assertThat(isUpdated).isTrue();
        verify(hotelRepository,times(1)).save(any(Hotel.class));
    }

    @Test
    void getAllHotelTest() {
        when(hotelRepository.findAll()).thenReturn(List.of(hotel));
        List<Hotel> hotels = hotelService.getAllHotel();
        assertEquals(1, hotels.size());
    }

}

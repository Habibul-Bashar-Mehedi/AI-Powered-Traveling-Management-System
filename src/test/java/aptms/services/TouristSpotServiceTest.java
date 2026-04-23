package aptms.services;

import aptms.entities.Destination;
import aptms.entities.TouristSpot;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.TouristSpotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TouristSpotServiceTest {

    @Mock
    private TouristSpotRepository touristSpotRepository;

    @InjectMocks
    private TouristSpotService touristSpotService;

    private Destination testDestination;
    private TouristSpot testSpot;

    @BeforeEach
    void setUp() {
        testDestination = new Destination();
        testDestination.setId(50L);

        testSpot = new TouristSpot();
        testSpot.setId(1L);
        testSpot.setName("Cox's Bazar Beach");
        testSpot.setDestination(testDestination);
        testSpot.setDescription("Longest sea beach");
        testSpot.setVisitingHours("24/7");
        testSpot.setAdultEntryFees(0.0);
        testSpot.setChildEntryFees(0.0);
        testSpot.setLocationDescription("Main beach point");
    }

    @Test
    void addTouristSpotTest() {
        when(touristSpotRepository.existsByNameAndDestinationId(anyString(), anyLong())).thenReturn(false);
        when(touristSpotRepository.save(any(TouristSpot.class))).thenReturn(testSpot);

        TouristSpot result = touristSpotService.addTouristSpot(testSpot);

        assertNotNull(result);
        assertEquals("Cox's Bazar Beach", result.getName());
        verify(touristSpotRepository, times(1)).save(testSpot);
    }

    @Test
    void missingTouristSpotTest() {
        testSpot.setDestination(null);
        assertThrows(InvalidException.class, () -> touristSpotService.addTouristSpot(testSpot));
    }

    @Test
    void alreadyExistsTest() {
        when(touristSpotRepository.existsByNameAndDestinationId(anyString(), anyLong())).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> touristSpotService.addTouristSpot(testSpot));
    }

    @Test
    void getAllTouristSpotTest() {
        when(touristSpotRepository.findAll()).thenReturn(List.of(testSpot));
        List<TouristSpot> spots = touristSpotService.getAllTouristSpot();
        assertEquals(1, spots.size());
    }

    @Test
    void deleteTouristSpotTest() {
        when(touristSpotRepository.existsById(1L)).thenReturn(true);

        String response = touristSpotService.deleteTouristSpot(1L);

        assertEquals("tourist spot is deleted", response);
        verify(touristSpotRepository, times(1)).deleteById(1L);
    }

    @Test
    void updateTouristSpotTest() {
        when(touristSpotRepository.findById(1L)).thenReturn(Optional.of(testSpot));

        boolean result = touristSpotService.updateTouristSpot(
                1L, "Saint Martin", "Coral Island", "8am-5pm", 100.0, 50.0, "Island Center"
        );

        assertTrue(result);
        assertEquals("Saint Martin", testSpot.getName());
        assertEquals(100.0, testSpot.getAdultEntryFees());
        verify(touristSpotRepository, times(1)).save(testSpot);
    }
}
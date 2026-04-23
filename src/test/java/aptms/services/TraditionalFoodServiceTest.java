package aptms.services;

import aptms.entities.Destination;
import aptms.entities.TraditionalFood;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.TraditionalFoodRepository;
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
public class TraditionalFoodServiceTest {

    @Mock
    private TraditionalFoodRepository traditionalFoodRepository;

    @InjectMocks
    private TraditionalFoodService traditionalFoodService;

    private Destination testDestination;
    private TraditionalFood testFood;

    @BeforeEach
    void setUp() {
        testDestination = new Destination();
        testDestination.setId(20L);

        testFood = new TraditionalFood();
        testFood.setId(1L);
        testFood.setDishName("Kacchi Biryani");
        testFood.setDestination(testDestination);
        testFood.setDescription("Famous mutton biryani with potato");
        testFood.setCulturalContext("Traditional wedding food in Old Dhaka");
        testFood.setPriceRange("300-500 BDT");
        testFood.setRecommendedLocation("Old Dhaka");
    }

    @Test
    void addTraditionalFoodTest() {
        when(traditionalFoodRepository.existsByDishNameAndDestinationId(anyString(), anyLong())).thenReturn(false);
        when(traditionalFoodRepository.save(any(TraditionalFood.class))).thenReturn(testFood);

        TraditionalFood result = traditionalFoodService.addTraditionalFood(testFood);

        assertNotNull(result);
        assertEquals("Kacchi Biryani", result.getDishName());
        verify(traditionalFoodRepository, times(1)).save(testFood);
    }

    @Test
    void missingFoodTest() {
        testFood.setDishName(null);
        assertThrows(InvalidException.class, () -> traditionalFoodService.addTraditionalFood(testFood));
    }

    @Test
    void alreadyExistsTest() {
        when(traditionalFoodRepository.existsByDishNameAndDestinationId(anyString(), anyLong())).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> traditionalFoodService.addTraditionalFood(testFood));
    }

    @Test
    void getAllTraditionalFoodTest() {
        when(traditionalFoodRepository.findAll()).thenReturn(List.of(testFood));
        List<TraditionalFood> foods = traditionalFoodService.getAllTraditionalFood();
        assertEquals(1, foods.size());
    }

    @Test
    void deleteTraditionalFoodTest() {
        when(traditionalFoodRepository.existsById(1L)).thenReturn(true);

        String response = traditionalFoodService.deleteTraditionalFood(1L);

        assertEquals("traditional food is deleted", response);
        verify(traditionalFoodRepository, times(1)).deleteById(1L);
    }

    @Test
    void updateTraditionalFoodTest() {
        when(traditionalFoodRepository.findById(1L)).thenReturn(Optional.of(testFood));

        boolean result = traditionalFoodService.updateTraditionalFood(
                1L, "Beef Tehari", "Spicy beef and rice", "Street food culture", "150-250 BDT", "Chawkbazar"
        );

        assertTrue(result);
        assertEquals("Beef Tehari", testFood.getDishName());
        assertEquals("150-250 BDT", testFood.getPriceRange());
        verify(traditionalFoodRepository, times(1)).save(testFood);
    }
}
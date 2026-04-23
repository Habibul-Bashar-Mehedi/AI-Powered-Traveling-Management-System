package aptms.services;

import aptms.entities.Destination;
import aptms.entities.Transport;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.DestinationRepository;
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
public class DestinationServiceTest {
    @Mock
    private DestinationRepository destinationRepository;

    @InjectMocks
    private DestinationService destinationService;

    private Destination testDestination;

    @BeforeEach
    void setUp() {
        testDestination = new Destination();
        testDestination.setId(1L);
        testDestination.setName("dhaka");
        testDestination.setRegion("BD");
        testDestination.setDescription("dhaka is a beautiful city");
        testDestination.setAlive(true);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        testDestination.setCreatedAt(calendar.getTime());
    }

    @Test
    void addDestinationTest() {
        when(destinationRepository
                .existsByNameAndRegion(
                        testDestination.getName(),
                        testDestination.getRegion())
        ).thenReturn(false);

        when(destinationRepository.save(any(Destination.class))).thenReturn(testDestination);

        Destination result = destinationService.addDestination(testDestination);

        assertThat(result).isNotNull();

        verify(destinationRepository,times(1)).save(testDestination);
    }

    @Test
    void alreadyExistsTest() {
        when(destinationRepository.existsByNameAndRegion(
                testDestination.getName(),
                testDestination.getRegion())
        ).thenReturn(true);

        assertThrows(DuplicateValueFoundExceptions.class,()->{
            destinationService.addDestination(testDestination);
        });

        verify(destinationRepository,never()).save(testDestination);
    }

    @Test
    void missingDestinationTest() {
        testDestination.setName(null);

        assertThrows(InvalidException.class,()->{
            destinationService.addDestination(testDestination);
        });
    }

    @Test
    void deleteDestinationTest() {
        long id = 1L;

        when(destinationRepository.existsById(id)).thenReturn(true);

        String response = destinationService.deleteDestination(id);

        assertEquals("destination is deleted",response);
        verify(destinationRepository,times(1)).deleteById(id);
    }

    @Test
    void updateTest() {
        long id = 1L;
        String newName="barishal";
        String newRegion = "Bangladesh";
        String newDescription = "River village";

        when(destinationRepository.findById(id)).thenReturn(java.util.Optional.of(testDestination));

        boolean isUpdate = destinationService.updateDestination(id,newName,newRegion,newDescription);

        assertThat(isUpdate).isTrue();

        verify(destinationRepository,times(1)).save(any(Destination.class));

    }

    @Test
    void getAllDestinationTest() {
        when(destinationRepository.findAll()).thenReturn(List.of(testDestination));
        List<Destination> destinations = destinationService.getAllDestinations();
        assertEquals(1, destinations.size());
    }
}

package aptms.services;

import aptms.entities.Destination;
import aptms.entities.Transport;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.InvalidException;
import aptms.repositories.TransportRepository;
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
public class TransportServiceTest {

    @Mock
    private TransportRepository transportRepository;

    @InjectMocks
    private TransportService transportService;

    private Destination origin;
    private Destination destination;
    private Transport testTransport;

    @BeforeEach
    void setUp() {
        origin = new Destination();
        origin.setId(1L);

        destination = new Destination();
        destination.setId(2L);

        testTransport = new Transport();
        testTransport.setId(10L);
        testTransport.setOrigin(origin);
        testTransport.setDestination(destination);
        testTransport.setModel("Scania Multi-axle");
        testTransport.setOperatorName("Green Line");
        testTransport.setEstimatedCost(1500.0);
        testTransport.setEstimatedDuration("6 Hours");
        testTransport.setFrequency("Daily");
    }

    @Test
    void addTransportTest() {
        when(transportRepository.existsByOriginIdAndDestinationId(anyLong(), anyLong())).thenReturn(false);
        when(transportRepository.save(any(Transport.class))).thenReturn(testTransport);

        Transport result = transportService.addTransport(testTransport);

        assertNotNull(result);
        assertEquals("Scania Multi-axle", result.getModel());
        verify(transportRepository, times(1)).save(testTransport);
    }

    @Test
    void missingTransportTest() {
        testTransport.setOrigin(null);
        assertThrows(InvalidException.class, () -> transportService.addTransport(testTransport));
    }

    @Test
    void alreadyExistsTest() {
        when(transportRepository.existsByOriginIdAndDestinationId(anyLong(), anyLong())).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> transportService.addTransport(testTransport));
    }

    @Test
    void getAllTransportTest() {
        when(transportRepository.findAll()).thenReturn(List.of(testTransport));
        List<Transport> transports = transportService.getAllTransport();
        assertEquals(1, transports.size());
    }

    @Test
    void deleteTransportTest() {
        when(transportRepository.existsById(10L)).thenReturn(true);

        String response = transportService.deleteTransport(10L);

        assertEquals("transport is deleted", response);
        verify(transportRepository, times(1)).deleteById(10L);
    }

    @Test
    void updateTransportTest() {
        when(transportRepository.findById(10L)).thenReturn(Optional.of(testTransport));

        boolean result = transportService.updateTransport(
                10L, "Hino AK1J", "Hanif Enterprise", 1200.0, "5.5 Hours", "Hourly"
        );

        assertTrue(result);
        assertEquals("Hino AK1J", testTransport.getModel());
        assertEquals(1200.0, testTransport.getEstimatedCost());
        verify(transportRepository, times(1)).save(testTransport);
    }
}
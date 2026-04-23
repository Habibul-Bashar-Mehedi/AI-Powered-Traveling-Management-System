package aptms.services;

import aptms.entities.Destination;
import aptms.entities.Route;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RouteRepository;
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
public class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteService routeService;

    private Destination origin;
    private Destination destination;
    private Route testRoute;

    @BeforeEach
    void setUp() {
        origin = new Destination();
        origin.setId(1L);

        destination = new Destination();
        destination.setId(2L);

        testRoute = new Route();
        testRoute.setId(100L);
        testRoute.setOrigin(origin);
        testRoute.setDestination(destination);
        testRoute.setDistanceKM(250.0);
        testRoute.setEstimatedDuration("5 hours");
        testRoute.setRouteDescription("Main Highway");
    }

    @Test
    void addRouteTest() {
        when(routeRepository.existsRouteByOriginIdAndDestinationId(anyLong(), anyLong())).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenReturn(testRoute);

        Route result = routeService.addRoute(testRoute);

        assertNotNull(result);
        assertEquals(250.0, result.getDistanceKM());
        verify(routeRepository, times(1)).save(testRoute);
    }

    @Test
    void missingRouteTest() {
        testRoute.setOrigin(null);
        assertThrows(InvalidException.class, () -> routeService.addRoute(testRoute));
    }

    @Test
    void alreadyExistsTest() {
        when(routeRepository.existsRouteByOriginIdAndDestinationId(anyLong(), anyLong())).thenReturn(true);
        assertThrows(DuplicateValueFoundExceptions.class, () -> routeService.addRoute(testRoute));
    }

    @Test
    void getAllRouteTest() {
        when(routeRepository.findAll()).thenReturn(List.of(testRoute));
        List<Route> routes = routeService.getAllRoute();
        assertEquals(1, routes.size());
    }

    @Test
    void deleteRouteTest() {
        when(routeRepository.existsById(100L)).thenReturn(true);

        String response = routeService.deleteRoute(100L);

        assertEquals("route is deleted", response);
        verify(routeRepository, times(1)).deleteById(100L);
    }

    @Test
    void updateRouteTest() {
        when(routeRepository.findById(100L)).thenReturn(Optional.of(testRoute));

        boolean result = routeService.updateRoute(100L, 300.0, "6 hours", "Updated Description");

        assertTrue(result);
        assertEquals(300.0, testRoute.getDistanceKM());
        assertEquals("6 hours", testRoute.getEstimatedDuration());
        verify(routeRepository, times(1)).save(testRoute);
    }
}
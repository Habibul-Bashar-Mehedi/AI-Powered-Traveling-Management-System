package aptms.services;

import aptms.entities.Route;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {
    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public Route addRoute(Route route) {
        if(route.getOrigin() == null || route.getDestination() == null) {
            throw new InvalidException("origin and destination is required");
        }

        boolean exist = routeRepository
                .existsRouteByOriginIdAndDestinationId(
                        route.getOrigin().getId(),
                        route.getDestination().getId());

        if(exist) throw new DuplicateValueFoundExceptions("route already added");

        return routeRepository.save(route);

    }

    public List<Route> getAllRoute() {
        return routeRepository.findAll();
    }

    //route
    public String deleteRoute(long id) {
        if(!routeRepository.existsById(id)) throw new IdNotFoundException("Route id not found");

        routeRepository.deleteById(id);
        return "route is deleted";
    }

    public boolean updateRoute(long id,double distanceKM,
                               String estimatedDuration,String routeDescription) {

        return routeRepository.findById(id).map(route -> {

            route.setDistanceKM(distanceKM);
            route.setEstimatedDuration(estimatedDuration);
            route.setRouteDescription(routeDescription);

            routeRepository.save(route);
            return true;
        }).orElseThrow(() ->
                new IdNotFoundException("Route id not found")

        );

    }
}

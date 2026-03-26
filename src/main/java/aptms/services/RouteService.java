package aptms.services;

import aptms.entities.Route;
import aptms.repositories.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {
    @Autowired
    private RouteRepository routeRepository;

    public String addRoute(Route route) {
        if(route.getOrigin() == null || route.getDestination() == null) {
            throw new RuntimeException("origin and destination is required");
        }

        boolean exist = routeRepository
                .existsRouteByOriginIdAndDestinationId(
                        route.getOrigin().getId(),
                        route.getDestination().getId());

        if(exist) throw new RuntimeException("route already added");

        routeRepository.save(route);

        return "route successfully added";

    }

    public List<Route> getAllRoute() {
        return routeRepository.findAll();
    }
}

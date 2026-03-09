package ai_powered_traveling_management_system.route.services;

import ai_powered_traveling_management_system.route.entities.Route;
import ai_powered_traveling_management_system.route.repositoies.RouteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}

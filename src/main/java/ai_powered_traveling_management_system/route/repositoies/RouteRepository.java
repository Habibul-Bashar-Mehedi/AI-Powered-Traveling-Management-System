package ai_powered_traveling_management_system.route.repositoies;

import ai_powered_traveling_management_system.route.entities.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsRouteByOriginIdAndDestinationId(long id, long id1);
}

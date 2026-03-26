package aptms.repositories;

import aptms.entities.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
    boolean existsRouteByOriginIdAndDestinationId(long id, long id1);
}

package aptms.repositories;

import aptms.entities.TouristSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot,Long> {
    boolean existsByNameAndDestinationId(String name, long id);
}

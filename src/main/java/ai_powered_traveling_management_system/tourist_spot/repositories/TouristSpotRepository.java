package ai_powered_traveling_management_system.tourist_spot.repositories;

import ai_powered_traveling_management_system.tourist_spot.entities.TouristSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristSpotRepository extends JpaRepository<TouristSpot,Long> {
    boolean existsByNameAndDestinationId(String name, long id);
}

package ai_powered_traveling_management_system.destination.repositories;


import ai_powered_traveling_management_system.destination.entities.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination,Long> {

    boolean existsByNameAndRegion(String name,String region);
}

package aptms.repositories;


import aptms.entities.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination,Long> {

    boolean existsByNameAndRegion( String name,String region);
}

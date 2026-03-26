package aptms.repositories;

import aptms.entities.Transport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportRepository extends JpaRepository<Transport, Long> {
    boolean existsByOriginIdAndDestinationId(long id, long id1);
}

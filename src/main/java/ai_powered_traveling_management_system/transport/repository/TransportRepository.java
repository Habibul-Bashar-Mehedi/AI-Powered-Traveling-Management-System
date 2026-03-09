package ai_powered_traveling_management_system.transport.repository;

import ai_powered_traveling_management_system.transport.entities.Transport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportRepository extends JpaRepository<Transport, Long> {
    boolean existsByOriginIdAndDestinationId(long id, long id1);
}

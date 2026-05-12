package aptms.repositories;

import aptms.entities.ServiceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceAvailabilityRepository extends JpaRepository<ServiceAvailability, UUID> {

    List<ServiceAvailability> findByServiceServiceIdAndAvailableDateBetween(
            UUID serviceId, LocalDate from, LocalDate to);

    Optional<ServiceAvailability> findByServiceServiceIdAndAvailableDate(
            UUID serviceId, LocalDate date);
}


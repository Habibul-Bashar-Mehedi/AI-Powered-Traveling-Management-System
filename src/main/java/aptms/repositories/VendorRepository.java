package aptms.repositories;

import aptms.entities.Vendor;
import aptms.entities.User;
import aptms.enums.VendorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByUser(User user);

    Optional<Vendor> findByEmail(String email);

    List<Vendor> findByStatus(VendorStatus status);

    boolean existsByEmail(String email);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByTaxId(String taxId);

    @Query("SELECT v FROM Vendor v WHERE v.user.id = :userId")
    Optional<Vendor> findByUserId(UUID userId);
}


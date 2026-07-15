package aptms.repositories;

import aptms.entities.Package;
import aptms.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PackageRepository extends JpaRepository<Package, UUID> {

    @Query("SELECT p FROM Package p WHERE p.status = :status AND p.deletedAt IS NULL")
    Page<Package> findByStatus(PackageStatus status, Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.packageId = :packageId AND p.status = :status AND p.deletedAt IS NULL")
    Optional<Package> findByPackageIdAndStatus(UUID packageId, PackageStatus status);

    @Query("SELECT p FROM Package p WHERE p.deletedAt IS NULL")
    List<Package> findAllNotDeleted();
}

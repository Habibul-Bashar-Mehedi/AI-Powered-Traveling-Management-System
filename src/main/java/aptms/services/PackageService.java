package aptms.services;

import aptms.dto.PackageBookRequestDTO;
import aptms.dto.PackageBookingDTO;
import aptms.dto.PackageDTO;
import aptms.enums.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PackageService {

    /** Admin: every package regardless of status. */
    List<PackageDTO> getAllPackages();

    /** Public: PUBLISHED packages only. */
    Page<PackageDTO> getPublishedPackages(Pageable pageable);

    /** Admin detail — any status. */
    PackageDTO getPackageById(UUID packageId);

    /** Public detail — PUBLISHED only. */
    PackageDTO getPublishedPackageById(UUID packageId);

    PackageDTO createPackage(PackageDTO dto);

    PackageDTO updatePackage(UUID packageId, PackageDTO dto);

    void deletePackage(UUID packageId);

    /** Transitions status; publishing validates the package is actually bookable. */
    PackageDTO setStatus(UUID packageId, PackageStatus status);

    PackageBookingDTO bookPackage(UUID userId, UUID packageId, PackageBookRequestDTO request);
}

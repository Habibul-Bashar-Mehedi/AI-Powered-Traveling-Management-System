package aptms.services.impl;

import aptms.dto.PackageBookRequestDTO;
import aptms.dto.PackageBookingDTO;
import aptms.dto.PackageComponentDTO;
import aptms.dto.PackageDTO;
import aptms.dto.PackageExtraDTO;
import aptms.dto.vendor.VendorBookingDTO;
import aptms.entities.Destination;
import aptms.entities.PackageBooking;
import aptms.entities.PackageComponent;
import aptms.entities.PackageExtra;
import aptms.entities.User;
import aptms.entities.VendorBooking;
import aptms.entities.VendorService;
import aptms.enums.PackageStatus;
import aptms.enums.ServiceStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.DestinationRepository;
import aptms.repositories.PackageBookingRepository;
import aptms.repositories.PackageRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.PackageService;
import aptms.services.ServiceCatalogService;
import aptms.services.VendorBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;
    private final VendorServiceRepository vendorServiceRepository;
    private final DestinationRepository destinationRepository;
    private final UserRepository userRepository;
    private final PackageBookingRepository packageBookingRepository;
    private final VendorBookingRepository vendorBookingRepository;
    private final ServiceCatalogService serviceCatalogService;
    private final VendorBookingService vendorBookingService;

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getAllPackages() {
        return packageRepository.findAllNotDeleted().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PackageDTO> getPublishedPackages(Pageable pageable) {
        return packageRepository.findByStatus(PackageStatus.PUBLISHED, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageDTO getPackageById(UUID packageId) {
        return toDTO(findById(packageId));
    }

    @Override
    @Transactional(readOnly = true)
    public PackageDTO getPublishedPackageById(UUID packageId) {
        aptms.entities.Package entity = packageRepository.findByPackageIdAndStatus(packageId, PackageStatus.PUBLISHED)
                .orElseThrow(() -> new IdNotFoundException("Published package not found: " + packageId));
        return toDTO(entity);
    }

    @Override
    @Transactional
    public PackageDTO createPackage(PackageDTO dto) {
        aptms.entities.Package entity = new aptms.entities.Package();
        mapDtoToEntity(dto, entity);
        return toDTO(packageRepository.save(entity));
    }

    @Override
    @Transactional
    public PackageDTO updatePackage(UUID packageId, PackageDTO dto) {
        aptms.entities.Package entity = findById(packageId);
        mapDtoToEntity(dto, entity);
        return toDTO(packageRepository.save(entity));
    }

    @Override
    @Transactional
    public void deletePackage(UUID packageId) {
        aptms.entities.Package entity = findById(packageId);
        entity.setDeletedAt(Instant.now());
        packageRepository.save(entity);
    }

    @Override
    @Transactional
    public PackageDTO setStatus(UUID packageId, PackageStatus status) {
        aptms.entities.Package entity = findById(packageId);
        if (status == PackageStatus.PUBLISHED) {
            validatePublishable(entity);
        }
        entity.setStatus(status);
        return toDTO(packageRepository.save(entity));
    }

    private void validatePublishable(aptms.entities.Package entity) {
        List<String> problems = new ArrayList<>();
        if (entity.getComponents() == null || entity.getComponents().isEmpty()) {
            problems.add("add at least one component");
        } else {
            for (PackageComponent component : entity.getComponents()) {
                VendorService service = component.getService();
                if (service == null || service.getStatus() != ServiceStatus.ACTIVE) {
                    problems.add("component '" + (service != null ? service.getServiceName() : "unknown")
                            + "' must reference an ACTIVE service");
                }
            }
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            problems.add("name is required");
        }
        if (entity.getDescription() == null || entity.getDescription().isBlank()) {
            problems.add("description is required");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Cannot publish this package: " + String.join("; ", problems) + ".");
        }
    }

    @Override
    @Transactional
    public PackageBookingDTO bookPackage(UUID userId, UUID packageId, PackageBookRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdNotFoundException("User not found: " + userId));

        aptms.entities.Package pkg = packageRepository.findByPackageIdAndStatus(packageId, PackageStatus.PUBLISHED)
                .orElseThrow(() -> new IdNotFoundException("Published package not found: " + packageId));

        // Reserves every component's capacity immediately (same pessimistic lock as
        // always) but leaves the header financially "open" — paymentStatus stays PENDING
        // until a real SSLCommerz checkout resolves it (see PaymentController/PaymentService).
        PackageBooking header = new PackageBooking();
        header.setPackageEntity(pkg);
        header.setUser(user);
        header.setStartDate(request.getStartDate());
        header.setTotalGrossAmount(BigDecimal.ZERO);
        header.setTotalCommissionAmount(BigDecimal.ZERO);
        header.setTotalNetAmount(BigDecimal.ZERO);
        header = packageBookingRepository.save(header);

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal commissionTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        List<VendorBooking> componentBookings = new ArrayList<>();

        // Reserving components one by one inside this single @Transactional method is what
        // makes the whole package atomic: any component that fails capacity/availability
        // throws, and Spring rolls back every reservation already made here (including this
        // header row) in one go, releasing every pessimistic lock taken so far.
        for (PackageComponent component : pkg.getComponents()) {
            java.time.LocalDate componentStart = component.getDayNumber() != null
                    ? request.getStartDate().plusDays(component.getDayNumber() - 1L)
                    : request.getStartDate();

            VendorBooking booking = serviceCatalogService.reserveComponent(
                    user,
                    component.getService().getServiceId(),
                    componentStart,
                    componentStart,
                    component.getQuantity() != null ? component.getQuantity() : 1,
                    "Part of package: " + pkg.getName());

            booking.setPackageBooking(header);
            booking = vendorBookingRepository.save(booking);

            grossTotal = grossTotal.add(booking.getGrossAmount());
            commissionTotal = commissionTotal.add(booking.getCommissionAmount());
            netTotal = netTotal.add(booking.getNetAmount());
            componentBookings.add(booking);
        }

        header.setTotalGrossAmount(grossTotal);
        header.setTotalCommissionAmount(commissionTotal);
        header.setTotalNetAmount(netTotal);
        header = packageBookingRepository.save(header);

        return toBookingDTO(header, componentBookings, user);
    }

    private aptms.entities.Package findById(UUID packageId) {
        aptms.entities.Package entity = packageRepository.findById(packageId)
                .orElseThrow(() -> new IdNotFoundException("Package not found: " + packageId));
        if (entity.getDeletedAt() != null) {
            throw new IdNotFoundException("Package not found: " + packageId);
        }
        return entity;
    }

    private Destination resolveDestination(Long destinationId) {
        if (destinationId == null) return null;
        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + destinationId));
    }

    private VendorService resolveService(UUID serviceId) {
        return vendorServiceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
    }

    private void mapDtoToEntity(PackageDTO dto, aptms.entities.Package entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setImageUrl(dto.getImageUrl());
        entity.setDestination(resolveDestination(dto.getDestinationId()));
        entity.setTotalPrice(dto.getTotalPrice());
        entity.setCurrencyCode(dto.getCurrencyCode() != null ? dto.getCurrencyCode() : "USD");
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }

        // Replace-all: clear the managed collections (orphanRemoval deletes the old rows)
        // and rebuild them from what the admin submitted this time — same convention as
        // VendorServiceMgmtServiceImpl's packageItems handling.
        entity.getComponents().clear();
        List<PackageComponentDTO> components = dto.getComponents();
        if (components != null) {
            for (PackageComponentDTO componentDto : components) {
                PackageComponent component = new PackageComponent();
                component.setPackageEntity(entity);
                component.setService(resolveService(componentDto.getServiceId()));
                component.setQuantity(componentDto.getQuantity() != null ? componentDto.getQuantity() : 1);
                component.setDayNumber(componentDto.getDayNumber());
                component.setSequence(componentDto.getSequence() != null ? componentDto.getSequence() : 0);
                component.setNotes(componentDto.getNotes());
                entity.getComponents().add(component);
            }
        }

        entity.getExtras().clear();
        List<PackageExtraDTO> extras = dto.getExtras();
        if (extras != null) {
            for (PackageExtraDTO extraDto : extras) {
                PackageExtra extra = new PackageExtra();
                extra.setPackageEntity(entity);
                extra.setTitle(extraDto.getTitle());
                extra.setDescription(extraDto.getDescription());
                extra.setPrice(extraDto.getPrice());
                extra.setIncluded(extraDto.getIncluded() != null ? extraDto.getIncluded() : true);
                entity.getExtras().add(extra);
            }
        }
    }

    private PackageDTO toDTO(aptms.entities.Package e) {
        PackageDTO dto = new PackageDTO();
        dto.setPackageId(e.getPackageId());
        dto.setName(e.getName());
        dto.setDescription(e.getDescription());
        dto.setImageUrl(e.getImageUrl());
        if (e.getDestination() != null) {
            dto.setDestinationId(e.getDestination().getId());
            dto.setDestinationName(e.getDestination().getName());
        }
        dto.setTotalPrice(e.getTotalPrice());
        dto.setCurrencyCode(e.getCurrencyCode());
        dto.setStatus(e.getStatus());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setComponents(e.getComponents() == null ? Collections.emptyList()
                : e.getComponents().stream().map(this::toComponentDTO).collect(Collectors.toList()));
        dto.setExtras(e.getExtras() == null ? Collections.emptyList()
                : e.getExtras().stream().map(this::toExtraDTO).collect(Collectors.toList()));
        return dto;
    }

    private PackageComponentDTO toComponentDTO(PackageComponent c) {
        PackageComponentDTO dto = new PackageComponentDTO();
        dto.setComponentId(c.getComponentId());
        VendorService service = c.getService();
        if (service != null) {
            dto.setServiceId(service.getServiceId());
            dto.setServiceName(service.getServiceName());
            dto.setServiceType(service.getServiceType() != null ? service.getServiceType().name() : null);
            dto.setBasePrice(service.getBasePrice());
            dto.setCurrencyCode(service.getCurrencyCode());
            dto.setMaxCapacity(service.getMaxCapacity());
        }
        dto.setQuantity(c.getQuantity());
        dto.setDayNumber(c.getDayNumber());
        dto.setSequence(c.getSequence());
        dto.setNotes(c.getNotes());
        return dto;
    }

    private PackageExtraDTO toExtraDTO(PackageExtra e) {
        PackageExtraDTO dto = new PackageExtraDTO();
        dto.setExtraId(e.getExtraId());
        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setPrice(e.getPrice());
        dto.setIncluded(e.getIncluded());
        return dto;
    }

    private PackageBookingDTO toBookingDTO(PackageBooking header, List<VendorBooking> componentBookings, User user) {
        PackageBookingDTO dto = new PackageBookingDTO();
        dto.setPackageBookingId(header.getPackageBookingId());
        dto.setPackageId(header.getPackageEntity().getPackageId());
        dto.setPackageName(header.getPackageEntity().getName());
        dto.setStartDate(header.getStartDate());
        dto.setTotalGrossAmount(header.getTotalGrossAmount());
        dto.setTotalCommissionAmount(header.getTotalCommissionAmount());
        dto.setTotalNetAmount(header.getTotalNetAmount());
        dto.setPaymentStatus(header.getPaymentStatus());
        dto.setPaymentMethod(header.getPaymentMethod());
        dto.setPaymentReference(header.getPaymentReference());
        dto.setCreatedAt(header.getCreatedAt());
        List<VendorBookingDTO> mapped = new ArrayList<>();
        for (VendorBooking booking : componentBookings) {
            mapped.add(vendorBookingService.mapBookingForUser(booking, user));
        }
        dto.setComponentBookings(mapped);
        return dto;
    }
}

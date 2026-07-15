package aptms.services.impl;

import aptms.dto.vendor.PackageItemDTO;
import aptms.dto.vendor.VendorServiceDTO;
import aptms.entities.Destination;
import aptms.entities.PackageItem;
import aptms.entities.Vendor;
import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import aptms.enums.VendorStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.DestinationRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.VendorServiceMgmtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceMgmtServiceImpl implements VendorServiceMgmtService {

    private final VendorServiceRepository serviceRepository;
    private final VendorRepository vendorRepository;
    private final VendorBookingRepository bookingRepository;
    private final DestinationRepository destinationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VendorServiceDTO> getMyServices(UUID userId) {
        Vendor vendor = getVendorByUserId(userId);
        return serviceRepository.findByVendorVendorId(vendor.getVendorId())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VendorServiceDTO createService(UUID userId, VendorServiceDTO dto) {
        Vendor vendor = getVendorByUserId(userId);
        assertNotSuspended(vendor);
        assertServiceTypeMatchesVendor(vendor, dto.getServiceType());
        VendorService entity = new VendorService();
        entity.setVendor(vendor);
        mapDtoToEntity(dto, entity);
        return toDTO(serviceRepository.save(entity));
    }

    @Override
    @Transactional
    public VendorServiceDTO updateService(UUID userId, UUID serviceId, VendorServiceDTO dto) {
        Vendor vendor = getVendorByUserId(userId);
        VendorService entity = serviceRepository
                .findByServiceIdAndVendorVendorId(serviceId, vendor.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
        mapDtoToEntity(dto, entity);
        return toDTO(serviceRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteService(UUID userId, UUID serviceId) {
        Vendor vendor = getVendorByUserId(userId);
        VendorService entity = serviceRepository
                .findByServiceIdAndVendorVendorId(serviceId, vendor.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
        if (bookingRepository.existsByServiceServiceId(serviceId)) {
            throw new IllegalStateException(
                    "This service has existing bookings and cannot be deleted. Set it to Inactive instead.");
        }
        serviceRepository.delete(entity);
        log.info("Service {} deleted by vendor {}", serviceId, vendor.getVendorId());
    }

    @Override
    @Transactional
    public VendorServiceDTO toggleServiceStatus(UUID userId, UUID serviceId, String status) {
        Vendor vendor = getVendorByUserId(userId);
        VendorService entity = serviceRepository
                .findByServiceIdAndVendorVendorId(serviceId, vendor.getVendorId())
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));
        entity.setStatus(ServiceStatus.valueOf(status));
        return toDTO(serviceRepository.save(entity));
    }

    private Vendor getVendorByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IdNotFoundException("Vendor not found for user: " + userId));
    }

    private Destination resolveDestination(Long destinationId) {
        if (destinationId == null) return null;
        return destinationRepository.findById(destinationId)
                .orElseThrow(() -> new IllegalArgumentException("Destination not found: " + destinationId));
    }

    private void assertServiceTypeMatchesVendor(Vendor vendor, ServiceType serviceType) {
        ServiceType allowed = vendor.getVendorType().toServiceType();
        if (serviceType != allowed) {
            throw new IllegalArgumentException(
                "Vendor type '" + vendor.getVendorType()
                    + "' can only create services of type '" + allowed
                    + "'. Requested: '" + serviceType + "'.");
        }
    }

    private void assertNotSuspended(Vendor vendor) {
        if (vendor.getStatus() == VendorStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Your vendor account is suspended and cannot create new services. " +
                    "Request reinstatement from your dashboard.");
        }
    }

    private void mapDtoToEntity(VendorServiceDTO dto, VendorService entity) {
        entity.setServiceName(dto.getServiceName());
        entity.setServiceType(dto.getServiceType());
        entity.setDescription(dto.getDescription());
        entity.setBasePrice(dto.getBasePrice());
        entity.setCurrencyCode(dto.getCurrencyCode() != null ? dto.getCurrencyCode() : "USD");
        entity.setPricingUnit(dto.getPricingUnit());
        entity.setMaxCapacity(dto.getMaxCapacity());
        entity.setMinBookingNotice(dto.getMinBookingNotice());
        entity.setMaxBookingAdvance(dto.getMaxBookingAdvance());
        entity.setBookingMode(dto.getBookingMode());
        entity.setConfirmationWindow(dto.getConfirmationWindow());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : ServiceStatus.DRAFT);
        entity.setCancellationPolicy(dto.getCancellationPolicy());
        entity.setLocationLat(dto.getLocationLat());
        entity.setLocationLng(dto.getLocationLng());
        entity.setLocationAddress(dto.getLocationAddress());
        entity.setDestination(resolveDestination(dto.getDestinationId()));
        entity.setTags(dto.getTags());
        entity.setMetadata(dto.getMetadata());
        entity.setImageUrl(dto.getImageUrl());

        // Replace-all: clear the managed collection (orphanRemoval deletes the old rows)
        // and rebuild it from what the vendor submitted this time.
        entity.getPackageItems().clear();
        List<PackageItemDTO> items = dto.getPackageItems();
        if (items != null) {
            for (PackageItemDTO itemDto : items) {
                PackageItem item = new PackageItem();
                item.setService(entity);
                item.setItemType(itemDto.getItemType());
                item.setTitle(itemDto.getTitle());
                item.setDescription(itemDto.getDescription());
                item.setDayNumber(itemDto.getDayNumber());
                item.setSequence(itemDto.getSequence() != null ? itemDto.getSequence() : 0);
                entity.getPackageItems().add(item);
            }
        }
    }

    private VendorServiceDTO toDTO(VendorService e) {
        VendorServiceDTO dto = new VendorServiceDTO();
        dto.setServiceId(e.getServiceId());
        dto.setServiceName(e.getServiceName());
        dto.setServiceType(e.getServiceType());
        dto.setDescription(e.getDescription());
        dto.setBasePrice(e.getBasePrice());
        dto.setCurrencyCode(e.getCurrencyCode());
        dto.setPricingUnit(e.getPricingUnit());
        dto.setMaxCapacity(e.getMaxCapacity());
        dto.setMinBookingNotice(e.getMinBookingNotice());
        dto.setMaxBookingAdvance(e.getMaxBookingAdvance());
        dto.setBookingMode(e.getBookingMode());
        dto.setConfirmationWindow(e.getConfirmationWindow());
        dto.setStatus(e.getStatus());
        dto.setCancellationPolicy(e.getCancellationPolicy());
        dto.setLocationLat(e.getLocationLat());
        dto.setLocationLng(e.getLocationLng());
        dto.setLocationAddress(e.getLocationAddress());
        if (e.getDestination() != null) {
            dto.setDestinationId(e.getDestination().getId());
            dto.setDestinationName(e.getDestination().getName());
        }
        dto.setTags(e.getTags());
        dto.setMetadata(e.getMetadata());
        dto.setImageUrl(e.getImageUrl());
        dto.setAverageRating(e.getAverageRating());
        dto.setTotalBookings(e.getTotalBookings());
        dto.setIsFeatured(e.getIsFeatured());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setPackageItems(e.getPackageItems() == null ? Collections.emptyList()
                : e.getPackageItems().stream().map(this::toItemDTO).collect(Collectors.toList()));
        return dto;
    }

    private PackageItemDTO toItemDTO(PackageItem item) {
        PackageItemDTO dto = new PackageItemDTO();
        dto.setItemId(item.getItemId());
        dto.setItemType(item.getItemType());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setDayNumber(item.getDayNumber());
        dto.setSequence(item.getSequence());
        return dto;
    }
}


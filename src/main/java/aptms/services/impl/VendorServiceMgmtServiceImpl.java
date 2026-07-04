package aptms.services.impl;

import aptms.dto.vendor.VendorServiceDTO;
import aptms.entities.Vendor;
import aptms.entities.VendorService;
import aptms.enums.ServiceStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.VendorRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.VendorServiceMgmtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceMgmtServiceImpl implements VendorServiceMgmtService {

    private final VendorServiceRepository serviceRepository;
    private final VendorRepository vendorRepository;

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
        entity.setTags(dto.getTags());
        entity.setMetadata(dto.getMetadata());
        entity.setImageUrl(dto.getImageUrl());
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
        dto.setTags(e.getTags());
        dto.setMetadata(e.getMetadata());
        dto.setImageUrl(e.getImageUrl());
        dto.setAverageRating(e.getAverageRating());
        dto.setTotalBookings(e.getTotalBookings());
        dto.setIsFeatured(e.getIsFeatured());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());
        return dto;
    }
}


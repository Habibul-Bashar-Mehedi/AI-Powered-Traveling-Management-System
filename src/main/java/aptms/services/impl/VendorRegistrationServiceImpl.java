package aptms.services.impl;

import aptms.dto.vendor.VendorProfileDTO;
import aptms.dto.vendor.VendorRegistrationRequest;
import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.UserRole;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.services.VendorRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorRegistrationServiceImpl implements VendorRegistrationService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public VendorProfileDTO register(VendorRegistrationRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() != UserRole.VENDOR) {
            user.setRole(UserRole.VENDOR);
            userRepository.save(user);
        }

        Optional<Vendor> existingVendor = vendorRepository.findByUser(user);
        if (existingVendor.isPresent()) {
            return toDTO(existingVendor.get());
        }

        Vendor vendor = new Vendor();
        vendor.setUser(user);
        mapRequestToVendor(request, vendor);

        Vendor saved = vendorRepository.save(vendor);
        log.info("Vendor registered: {} for user {}", saved.getVendorId(), userId);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorProfileDTO getProfile(UUID userId) {
        Vendor vendor = getVendorByUserId(userId);
        return toDTO(vendor);
    }

    @Override
    @Transactional
    public VendorProfileDTO updateProfile(UUID userId, VendorRegistrationRequest request) {
        Vendor vendor = getVendorByUserId(userId);
        mapRequestToVendor(request, vendor);
        return toDTO(vendorRepository.save(vendor));
    }

    private Vendor getVendorByUserId(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IdNotFoundException("Vendor profile not found for user: " + userId));
    }

    private void mapRequestToVendor(VendorRegistrationRequest request, Vendor vendor) {
        vendor.setBusinessName(request.getBusinessName());
        vendor.setVendorType(request.getVendorType());
        vendor.setRegistrationNumber(request.getRegistrationNumber());
        vendor.setTaxId(request.getTaxId());
        vendor.setDescription(request.getDescription());
        vendor.setEmail(request.getEmail());
        vendor.setPhone(request.getPhone());
        vendor.setWebsiteUrl(request.getWebsiteUrl());
        vendor.setAddressLine1(request.getAddressLine1());
        vendor.setAddressLine2(request.getAddressLine2());
        vendor.setCity(request.getCity());
        vendor.setStateProvince(request.getStateProvince());
        vendor.setCountryCode(request.getCountryCode());
        vendor.setPostalCode(request.getPostalCode());
    }

    public static VendorProfileDTO toDTO(Vendor v) {
        VendorProfileDTO dto = new VendorProfileDTO();
        dto.setVendorId(v.getVendorId());
        dto.setUserId(v.getUser() != null ? v.getUser().getId() : null);
        dto.setBusinessName(v.getBusinessName());
        dto.setVendorType(v.getVendorType());
        dto.setRegistrationNumber(v.getRegistrationNumber());
        dto.setTaxId(v.getTaxId());
        dto.setDescription(v.getDescription());
        dto.setLogoUrl(v.getLogoUrl());
        dto.setEmail(v.getEmail());
        dto.setPhone(v.getPhone());
        dto.setWebsiteUrl(v.getWebsiteUrl());
        dto.setAddressLine1(v.getAddressLine1());
        dto.setAddressLine2(v.getAddressLine2());
        dto.setCity(v.getCity());
        dto.setStateProvince(v.getStateProvince());
        dto.setCountryCode(v.getCountryCode());
        dto.setPostalCode(v.getPostalCode());
        dto.setStatus(v.getStatus());
        dto.setRejectionReason(v.getRejectionReason());
        dto.setCommissionRate(v.getCommissionRate());
        dto.setWalletBalance(v.getWalletBalance());
        dto.setPendingBalance(v.getPendingBalance());
        dto.setPayoutMethod(v.getPayoutMethod());
        dto.setAverageRating(v.getAverageRating());
        dto.setTotalReviews(v.getTotalReviews());
        dto.setIsEmailVerified(v.getIsEmailVerified());
        dto.setCreatedAt(v.getCreatedAt());
        dto.setApprovedAt(v.getApprovedAt());
        return dto;
    }
}


package aptms.services;

import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import aptms.repositories.AdminOrderRepository;
import aptms.repositories.ProductRepository;
import aptms.repositories.SystemSettingRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.services.impl.AdminDashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AdminOrderRepository adminOrderRepository;
    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Test
    void rejectVendorShouldPersistStatusAndReason() {
        UUID vendorId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        User actor = new User();
        actor.setId(actorId);
        actor.setRole(UserRole.ADMIN);

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Vendor vendor = new Vendor();
        vendor.setVendorId(vendorId);
        vendor.setUser(owner);
        vendor.setBusinessName("Sample Vendor");
        vendor.setEmail("vendor@test.com");
        vendor.setPhone("01700000000");
        vendor.setStatus(VendorStatus.PENDING);
        vendor.setCreatedAt(Instant.now());

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(vendorRepository.save(vendor)).thenReturn(vendor);

        var response = adminDashboardService.rejectVendor(vendorId, actorId, " Missing KYC documents ");

        assertThat(vendor.getStatus()).isEqualTo(VendorStatus.REJECTED);
        assertThat(vendor.getRejectionReason()).isEqualTo("Missing KYC documents");
        assertThat(response.status()).isEqualTo(VendorStatus.REJECTED);
    }
}


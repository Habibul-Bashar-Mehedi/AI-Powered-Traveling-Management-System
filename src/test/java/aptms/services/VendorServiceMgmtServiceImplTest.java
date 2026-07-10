package aptms.services;

import aptms.entities.Vendor;
import aptms.entities.VendorService;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.impl.VendorServiceMgmtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceMgmtServiceImplTest {

    @Mock
    private VendorServiceRepository serviceRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorBookingRepository bookingRepository;

    @InjectMocks
    private VendorServiceMgmtServiceImpl vendorServiceMgmtService;

    private UUID userId;
    private UUID serviceId;
    private Vendor vendor;
    private VendorService service;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        serviceId = UUID.randomUUID();

        vendor = new Vendor();
        vendor.setVendorId(UUID.randomUUID());

        service = new VendorService();
        service.setServiceId(serviceId);
        service.setVendor(vendor);

        when(vendorRepository.findByUserId(userId)).thenReturn(Optional.of(vendor));
        when(serviceRepository.findByServiceIdAndVendorVendorId(serviceId, vendor.getVendorId()))
                .thenReturn(Optional.of(service));
    }

    @Test
    void deleteService_throwsAndSkipsDelete_whenServiceHasExistingBookings() {
        when(bookingRepository.existsByServiceServiceId(serviceId)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> vendorServiceMgmtService.deleteService(userId, serviceId));

        verify(serviceRepository, never()).delete(any());
    }

    @Test
    void deleteService_deletesService_whenNoBookingsExist() {
        when(bookingRepository.existsByServiceServiceId(serviceId)).thenReturn(false);

        vendorServiceMgmtService.deleteService(userId, serviceId);

        verify(serviceRepository).delete(service);
    }
}

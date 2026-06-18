package aptms.config;

import aptms.entities.User;
import aptms.entities.Vendor;
import aptms.entities.VendorService;
import aptms.enums.BookingMode;
import aptms.enums.PayoutMethod;
import aptms.enums.PricingUnit;
import aptms.enums.ServiceStatus;
import aptms.enums.ServiceType;
import aptms.enums.UserRole;
import aptms.enums.VendorStatus;
import aptms.enums.VendorType;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.VendorServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Seeds test accounts on application startup when the "dev" profile is active.
 * <p>
 * Test credentials (all passwords meet the 8-char minimum):
 * <ul>
 *   <li>VENDOR  ▸  vendor@test.com   /  Vendor@123</li>
 *   <li>ADMIN   ▸  admin@test.com    /  Admin@123</li>
 *   <li>USER    ▸  user@test.com     /  User@1234</li>
 * </ul>
 * The vendor account comes pre-approved with a fully populated Vendor profile
 * so you can log in and use the dashboard immediately.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class TestDataSeeder implements CommandLineRunner {

    private final UserRepository    userRepository;
    private final VendorRepository  vendorRepository;
    private final VendorServiceRepository vendorServiceRepository;
    private final PasswordEncoder   passwordEncoder;

    @Override
    @Transactional
    public void run(@NonNull String... args) {
        seedAdmin();
        seedRegularUser();
        seedVendorUser();
        // Always ensure services exist, even if vendor was seeded in a prior run
        seedVendorServicesForExistingVendor();
        log.info("─────────────────────────────────────────────────────");
        log.info("  TEST ACCOUNTS READY");
        log.info("  VENDOR : vendor@test.com  /  Vendor@123");
        log.info("  ADMIN  : admin@test.com   /  Admin@123");
        log.info("  USER   : user@test.com    /  User@1234");
        log.info("─────────────────────────────────────────────────────");
    }

    /** Idempotent: seeds services for the test vendor regardless of when the vendor was created. */
    private void seedVendorServicesForExistingVendor() {
        userRepository.findByEmail("vendor@test.com").ifPresent(vendorUser ->
            vendorRepository.findByUserId(vendorUser.getId()).ifPresent(this::seedVendorServices)
        );
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    private void seedAdmin() {
        if (userRepository.existsByEmail("admin@test.com")) return;

        User admin = new User();
        admin.setUsername("admin_test");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRole(UserRole.ADMIN);
        admin.setCountryId("BD");
        userRepository.save(admin);
        log.info("Seeded ADMIN  : admin@test.com");
    }

    // ── Regular User ─────────────────────────────────────────────────────────

    private void seedRegularUser() {
        if (userRepository.existsByEmail("user@test.com")) return;

        User user = new User();
        user.setUsername("user_test");
        user.setEmail("user@test.com");
        user.setPassword(passwordEncoder.encode("User@1234"));
        user.setRole(UserRole.USER);
        user.setCountryId("BD");
        userRepository.save(user);
        log.info("Seeded USER   : user@test.com");
    }

    // ── Vendor User + Vendor Profile ─────────────────────────────────────────

    private void seedVendorUser() {
        if (userRepository.existsByEmail("vendor@test.com")) return;

        // 1. Create the user account with VENDOR role
        User vendorUser = new User();
        vendorUser.setUsername("vendor_test");
        vendorUser.setEmail("vendor@test.com");
        vendorUser.setPassword(passwordEncoder.encode("Vendor@123"));
        vendorUser.setRole(UserRole.VENDOR);
        vendorUser.setCountryId("BD");
        vendorUser = userRepository.save(vendorUser);

        // 2. Create a pre-approved Vendor profile linked to that user
        Vendor vendor = new Vendor();
        vendor.setUser(vendorUser);
        vendor.setBusinessName("Dhaka Express Tours");
        vendor.setVendorType(VendorType.TOUR_GUIDE);
        vendor.setRegistrationNumber("REG-TEST-001");
        vendor.setTaxId("TAX-TEST-001");
        vendor.setDescription(
            "A full-service tour operator based in Dhaka offering cultural and adventure packages across Bangladesh.");
        vendor.setEmail("vendor@test.com");
        vendor.setPhone("+8801712345678");
        vendor.setWebsiteUrl("https://dhakaexpresstours.example.com");
        vendor.setAddressLine1("12 Gulshan Avenue");
        vendor.setCity("Dhaka");
        vendor.setStateProvince("Dhaka Division");
        vendor.setCountryCode("BD");
        vendor.setPostalCode("1212");
        vendor.setStatus(VendorStatus.APPROVED);
        vendor.setApprovedAt(Instant.now());
        vendor.setApprovedBy(vendorUser);   // self-approved for test data
        vendor.setCommissionRate(new BigDecimal("10.00"));
        vendor.setWalletBalance(new BigDecimal("1250.00"));
        vendor.setPendingBalance(new BigDecimal("300.00"));
        vendor.setPayoutMethod(PayoutMethod.BANK_TRANSFER);
        vendor.setAverageRating(new BigDecimal("4.75"));
        vendor.setTotalReviews(42);
        vendor.setIsEmailVerified(true);
        vendorRepository.save(vendor);

        // 3. Seed active vendor services so user dashboard requests work immediately
        seedVendorServices(vendor);

        log.info("Seeded VENDOR : vendor@test.com  (profile: '{}')", vendor.getBusinessName());
    }

    // ── Vendor Services ───────────────────────────────────────────────────────

    private void seedVendorServices(Vendor vendor) {
        if (!vendorServiceRepository.findByVendorVendorId(vendor.getVendorId()).isEmpty()) return;

        // Hotel Room service — covers "Hotel Booking" dashboard action
        VendorService hotel = new VendorService();
        hotel.setVendor(vendor);
        hotel.setServiceName("Standard Hotel Room");
        hotel.setServiceType(ServiceType.HOTEL_ROOM);
        hotel.setDescription("Comfortable hotel room with all modern amenities in Dhaka city center.");
        hotel.setBasePrice(new BigDecimal("3500.00"));
        hotel.setCurrencyCode("BDT");
        hotel.setPricingUnit(PricingUnit.PER_NIGHT);
        hotel.setMaxCapacity(2);
        hotel.setBookingMode(BookingMode.MANUAL);
        hotel.setStatus(ServiceStatus.ACTIVE);
        hotel.setCancellationPolicy("Free cancellation up to 24 hours before check-in.");
        hotel.setLocationAddress("12 Gulshan Avenue, Dhaka, BD");
        vendorServiceRepository.save(hotel);

        // Tour Package service — covers "Explore Tourist Places" & "Order Traditional Food & Items"
        VendorService tour = new VendorService();
        tour.setVendor(vendor);
        tour.setServiceName("Bangladesh Heritage Tour Package");
        tour.setServiceType(ServiceType.TOUR_PACKAGE);
        tour.setDescription("Guided cultural tour through Dhaka's heritage sites, local markets, and traditional food spots.");
        tour.setBasePrice(new BigDecimal("5000.00"));
        tour.setCurrencyCode("BDT");
        tour.setPricingUnit(PricingUnit.PER_PERSON);
        tour.setMaxCapacity(10);
        tour.setBookingMode(BookingMode.MANUAL);
        tour.setStatus(ServiceStatus.ACTIVE);
        tour.setCancellationPolicy("Free cancellation up to 48 hours before the tour date.");
        tour.setLocationAddress("Dhaka, Bangladesh");
        tour.setIsFeatured(true);
        vendorServiceRepository.save(tour);

        // Transport service
        VendorService transport = new VendorService();
        transport.setVendor(vendor);
        transport.setServiceName("Airport Pickup & Drop-off");
        transport.setServiceType(ServiceType.TRANSPORT_ROUTE);
        transport.setDescription("Reliable AC vehicle airport transfer service across Dhaka.");
        transport.setBasePrice(new BigDecimal("1200.00"));
        transport.setCurrencyCode("BDT");
        transport.setPricingUnit(PricingUnit.PER_TRIP);
        transport.setMaxCapacity(4);
        transport.setBookingMode(BookingMode.MANUAL);
        transport.setStatus(ServiceStatus.ACTIVE);
        transport.setCancellationPolicy("Cancel at least 2 hours before pickup.");
        transport.setLocationAddress("Hazrat Shahjalal International Airport, Dhaka");
        vendorServiceRepository.save(transport);

        log.info("Seeded 3 active VendorServices for vendor '{}'", vendor.getBusinessName());
    }
}


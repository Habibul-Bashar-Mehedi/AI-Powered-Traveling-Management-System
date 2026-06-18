package aptms.services;

import aptms.annotations.SecureAction;
import aptms.entities.Booking;
import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.entities.VendorService;
import aptms.enums.BookingStatus;
import aptms.enums.ServiceType;
import aptms.enums.VendorBookingStatus;
import aptms.enums.VendorPaymentStatus;
import aptms.exceptions.DuplicateValueFoundExceptions;
import aptms.exceptions.IdNotFoundException;
import aptms.exceptions.InvalidException;
import aptms.repositories.BookingRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.VendorServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static aptms.constants.BookingConstants.*;
import static aptms.constants.EntityConstants.*;

@Service
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final VendorRepository vendorRepository;
    private final VendorBookingRepository vendorBookingRepository;
    private final VendorServiceRepository vendorServiceRepository;

    public BookingService(BookingRepository bookingRepository,
                          VendorRepository vendorRepository,
                          VendorBookingRepository vendorBookingRepository,
                          VendorServiceRepository vendorServiceRepository) {
        this.bookingRepository = bookingRepository;
        this.vendorRepository = vendorRepository;
        this.vendorBookingRepository = vendorBookingRepository;
        this.vendorServiceRepository = vendorServiceRepository;
    }

    @Transactional
    @SecureAction(role = "USER")
    public Booking booking(Booking booking) {
        if(booking.getRoom() == null || booking.getHotel() == null || booking.getUser() == null) {
            throw new InvalidException(BOOKING_VALIDATION_ERROR);
        }

        boolean isAlreadyBooked = bookingRepository.isRoomBooked(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                BookingStatus.CANCELLED
        );

        if(isAlreadyBooked){
            throw new DuplicateValueFoundExceptions(ROOM_ALREADY_BOOKED_MESSAGE);
        }
        
        // Set default status if not provided
        if(booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }

        Booking saved = bookingRepository.save(booking);

        // Mirror booking into vendor_booking so the hotel vendor can see and act on it
        createVendorBookingIfApplicable(saved);

        return saved;
    }

    /**
     * Creates a VendorBooking record so the hotel's vendor can see the pending booking
     * in their dashboard. Fails softly — if the hotel has no vendor profile or no
     * HOTEL_ROOM service listing, we log a warning and skip.
     */
    private void createVendorBookingIfApplicable(Booking booking) {
        try {
            if (booking.getHotel() == null || booking.getHotel().getVendor() == null) {
                log.warn("Hotel {} has no vendor assigned; skipping VendorBooking creation", booking.getHotel());
                return;
            }

            Optional<Vendor> vendorOpt = vendorRepository.findByUserId(booking.getHotel().getVendor().getId());
            if (vendorOpt.isEmpty()) {
                log.warn("No Vendor profile found for hotel vendor user {}; skipping VendorBooking creation",
                        booking.getHotel().getVendor().getId());
                return;
            }
            Vendor vendor = vendorOpt.get();

            Optional<VendorService> serviceOpt = vendorServiceRepository
                    .findFirstByVendorVendorIdAndServiceType(vendor.getVendorId(), ServiceType.HOTEL_ROOM);
            if (serviceOpt.isEmpty()) {
                log.warn("Vendor {} has no HOTEL_ROOM service listing; skipping VendorBooking creation",
                        vendor.getVendorId());
                return;
            }
            VendorService vendorService = serviceOpt.get();

            BigDecimal gross = BigDecimal.valueOf(booking.getTotalPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal commission = gross
                    .multiply(vendor.getCommissionRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal net = gross.subtract(commission);

            VendorBooking vb = new VendorBooking();
            vb.setVendor(vendor);
            vb.setService(vendorService);
            vb.setUser(booking.getUser());
            vb.setBookingStatus(VendorBookingStatus.PENDING);
            vb.setPaymentStatus(VendorPaymentStatus.PENDING);
            vb.setStartDate(booking.getCheckInDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
            vb.setEndDate(booking.getCheckOutDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
            vb.setQuantity(booking.getGuestCount());
            vb.setGrossAmount(gross);
            vb.setCommissionAmount(commission);
            vb.setNetAmount(net);
            vb.setSpecialRequests(booking.getSpecialRequest());

            vendorBookingRepository.save(vb);
            log.info("VendorBooking created for booking id={} vendor={}", booking.getId(), vendor.getVendorId());
        } catch (Exception e) {
            log.error("Failed to create VendorBooking for booking id={}: {}", booking.getId(), e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    @SecureAction(role = "ADMIN")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public String deleteBooking(long id) {
        if(!bookingRepository.existsById(id)) {
            throw new IdNotFoundException(
                String.format(ENTITY_NOT_FOUND_MESSAGE, BOOKING, id)
            );
        }

        bookingRepository.deleteById(id);
        return String.format(ENTITY_DELETED_MESSAGE, BOOKING);
    }

    @Transactional
    @SecureAction(role = "ADMIN")
    public boolean updateBooking(long id, Date checkInDate,
                                 Date checkOutDate, int guestCount,
                                 Double totalPrice, BookingStatus status,
                                 String specialRequest) {

        return bookingRepository.findById(id).map(booking -> {
            booking.setCheckInDate(checkInDate);
            booking.setCheckOutDate(checkOutDate);
            booking.setGuestCount(guestCount);
            booking.setTotalPrice(totalPrice);
            booking.setStatus(status);
            booking.setSpecialRequest(specialRequest);

            bookingRepository.save(booking);
            return true;
        }).orElseThrow(() ->
                new IdNotFoundException(
                    String.format(ENTITY_NOT_FOUND_MESSAGE, BOOKING, id)
                )
        );
    }
}

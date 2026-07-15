package aptms.services.impl;

import aptms.dto.ReceiptDTO;
import aptms.entities.Booking;
import aptms.entities.VendorBooking;
import aptms.enums.BookingSource;
import aptms.enums.BookingStatus;
import aptms.enums.ServiceType;
import aptms.enums.VendorBookingStatus;
import aptms.repositories.BookingRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.services.ReceiptService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final Set<BookingStatus> RECEIPT_ELIGIBLE_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.CHECKED_IN, BookingStatus.CHECKED_OUT);

    private static final Set<VendorBookingStatus> RECEIPT_ELIGIBLE_VENDOR_BOOKING_STATUSES =
            EnumSet.of(VendorBookingStatus.CONFIRMED, VendorBookingStatus.COMPLETED);

    private final BookingRepository bookingRepository;
    private final VendorBookingRepository vendorBookingRepository;
    private final TemplateEngine templateEngine;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReceipt(String bookingId, UUID userId) {
        ReceiptDTO receipt = resolveReceipt(bookingId, userId);
        String html = templateEngine.process("receipt", buildContext(receipt));
        return renderPdf(html);
    }

    private ReceiptDTO resolveReceipt(String bookingId, UUID userId) {
        Long numericId = parseLongOrNull(bookingId);
        if (numericId != null) {
            Booking booking = bookingRepository.findByIdAndUserId(numericId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
            return mapBooking(booking);
        }

        UUID uuid = parseUuidOrNull(bookingId);
        if (uuid != null) {
            VendorBooking booking = vendorBookingRepository.findByBookingIdAndUserId(uuid, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
            return mapVendorBooking(booking);
        }

        throw new IllegalArgumentException("Booking not found: " + bookingId);
    }

    private ReceiptDTO mapBooking(Booking booking) {
        if (!RECEIPT_ELIGIBLE_BOOKING_STATUSES.contains(booking.getStatus())) {
            throw new IllegalStateException("Receipt not available for bookings in " + booking.getStatus() + " state");
        }

        ReceiptDTO dto = new ReceiptDTO();
        dto.setId(String.valueOf(booking.getId()));
        dto.setSource(BookingSource.HOTEL_DIRECT);
        dto.setServiceType(ServiceType.HOTEL_ROOM);
        dto.setTitle(booking.getHotel().getHotelName() + " — " + booking.getRoom().getRoomTypeName());
        dto.setCustomerName(booking.getUser().getUsername());
        dto.setCustomerEmail(booking.getUser().getEmail());
        dto.setProviderName(booking.getHotel().getHotelName());
        dto.setProviderAddress(booking.getHotel().getAddress());
        dto.setBookingDate(booking.getCreatedAt().toInstant());
        dto.setTravelStartDate(toLocalDate(booking.getCheckInDate()));
        dto.setTravelEndDate(toLocalDate(booking.getCheckOutDate()));
        dto.setStatus(booking.getStatus().name());
        dto.setQuantity(booking.getGuestCount());
        dto.setUnitPrice(BigDecimal.valueOf(booking.getRoom().getPricePerNight()));
        dto.setAmount(BigDecimal.valueOf(booking.getTotalPrice()));
        dto.setCurrencyCode("USD");
        dto.setSpecialRequests(booking.getSpecialRequest());
        if (booking.getHotel().getDestination() != null) {
            dto.setDestinationName(booking.getHotel().getDestination().getName());
        }
        dto.setIssuedAt(Instant.now());
        return dto;
    }

    private ReceiptDTO mapVendorBooking(VendorBooking booking) {
        if (!RECEIPT_ELIGIBLE_VENDOR_BOOKING_STATUSES.contains(booking.getBookingStatus())) {
            throw new IllegalStateException("Receipt not available for bookings in " + booking.getBookingStatus() + " state");
        }

        ReceiptDTO dto = new ReceiptDTO();
        dto.setId(booking.getBookingId().toString());
        dto.setSource(BookingSource.VENDOR_SERVICE);
        dto.setServiceType(booking.getService().getServiceType());
        dto.setTitle(booking.getService().getServiceName());
        dto.setCustomerName(booking.getUser().getUsername());
        dto.setCustomerEmail(booking.getUser().getEmail());
        dto.setProviderName(booking.getVendor().getBusinessName());
        dto.setProviderAddress(buildVendorAddress(booking.getVendor().getAddressLine1(),
                booking.getVendor().getAddressLine2(), booking.getVendor().getCity(), booking.getVendor().getCountryCode()));
        dto.setBookingDate(booking.getCreatedAt());
        dto.setTravelStartDate(booking.getStartDate());
        dto.setTravelEndDate(booking.getEndDate());
        dto.setStatus(booking.getBookingStatus().name());
        dto.setPaymentStatus(booking.getPaymentStatus().name());
        if (booking.getPaymentMethod() != null) {
            dto.setPaymentMethod(booking.getPaymentMethod().name());
        }
        dto.setPaymentReference(booking.getPaymentReference());
        dto.setQuantity(booking.getQuantity());
        dto.setUnitPrice(booking.getService().getBasePrice());
        dto.setAmount(booking.getGrossAmount());
        dto.setCurrencyCode(booking.getService().getCurrencyCode());
        dto.setSpecialRequests(booking.getSpecialRequests());
        if (booking.getService().getDestination() != null) {
            dto.setDestinationName(booking.getService().getDestination().getName());
        }
        dto.setIssuedAt(Instant.now());
        return dto;
    }

    private String buildVendorAddress(String line1, String line2, String city, String countryCode) {
        StringBuilder sb = new StringBuilder(line1);
        if (line2 != null && !line2.isBlank()) {
            sb.append(", ").append(line2);
        }
        sb.append(", ").append(city).append(", ").append(countryCode);
        return sb.toString();
    }

    private Context buildContext(ReceiptDTO receipt) {
        Context context = new Context();
        context.setVariable("receipt", receipt);
        return context;
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render receipt PDF", e);
        }
    }

    private java.time.LocalDate toLocalDate(java.util.Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

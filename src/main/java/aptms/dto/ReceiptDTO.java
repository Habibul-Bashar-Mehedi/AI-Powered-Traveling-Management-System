package aptms.dto;

import aptms.enums.BookingSource;
import aptms.enums.ServiceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Unified receipt view for a single booking, covering both direct hotel
 * {@code Booking} and generic {@code VendorBooking} records. Mirrors
 * {@link BookingHistoryDTO}'s pattern of using a {@code String} id to paper
 * over the {@code Long} vs {@code UUID} primary-key mismatch between the two
 * underlying entities.
 */
@Data
public class ReceiptDTO {
    private String id;
    private BookingSource source;
    private ServiceType serviceType;
    private String title;

    private String customerName;
    private String customerEmail;

    private String providerName;
    private String providerAddress;

    private Instant bookingDate;
    private LocalDate travelStartDate;
    private LocalDate travelEndDate;

    private String status;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentReference;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String currencyCode;

    private String destinationName;
    private String specialRequests;

    private Instant issuedAt;
}

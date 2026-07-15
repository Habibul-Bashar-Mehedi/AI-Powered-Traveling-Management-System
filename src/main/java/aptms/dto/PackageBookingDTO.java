package aptms.dto;

import aptms.enums.PaymentMethod;
import aptms.enums.VendorPaymentStatus;
import aptms.dto.vendor.VendorBookingDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Result of booking a Package — the header plus each component's own VendorBooking. */
@Data
public class PackageBookingDTO {

    private UUID packageBookingId;
    private UUID packageId;
    private String packageName;
    private LocalDate startDate;

    private BigDecimal totalGrossAmount;
    private BigDecimal totalCommissionAmount;
    private BigDecimal totalNetAmount;

    private VendorPaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String paymentReference;

    private Instant createdAt;

    private List<VendorBookingDTO> componentBookings;
}

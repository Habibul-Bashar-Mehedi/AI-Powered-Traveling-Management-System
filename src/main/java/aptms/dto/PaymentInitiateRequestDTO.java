package aptms.dto;

import aptms.enums.PaymentBookingType;
import aptms.enums.PreferredPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentInitiateRequestDTO {

    @NotNull(message = "Booking type is required")
    private PaymentBookingType bookingType;

    @NotNull(message = "Booking id is required")
    private UUID bookingId;

    // Optional — a filter hint for SSLCommerz's hosted page, not a hard requirement.
    private PreferredPaymentMethod preferredMethod;

    // Optional — a more specific provider within preferredMethod (e.g. "BKASH", "VISA").
    // Only a small known set is recognized (see PaymentServiceImpl#toMultiCardName);
    // anything else falls back to the category-level preferredMethod filter.
    private String preferredProvider;

    // Optional — Bangladeshi mobile number collected in the Mobile Banking sub-step.
    // Just contact info passed to SSLCommerz as cus_phone, never a PIN/OTP/credential.
    @Pattern(regexp = "^01[3-9][0-9]{8}$", message = "Enter a valid Bangladeshi mobile number (e.g. 01XXXXXXXXX)")
    private String contactPhone;
}

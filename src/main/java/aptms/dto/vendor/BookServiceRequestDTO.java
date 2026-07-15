package aptms.dto.vendor;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookServiceRequestDTO {

    @NotNull(message = "A booking date is required")
    @FutureOrPresent(message = "The booking date cannot be in the past")
    private LocalDate startDate;

    @FutureOrPresent(message = "The end date cannot be in the past")
    private LocalDate endDate;

    @Min(1)
    private Integer quantity = 1;

    @Size(max = 1000)
    private String specialRequests;

    @Size(max = 500)
    private String deliveryAddress;

    @Size(max = 30)
    private String contactPhone;

    // Payment method/reference are no longer collected here — checkout now happens
    // via a real SSLCommerz redirect (see PaymentController), not a self-declared field.
}

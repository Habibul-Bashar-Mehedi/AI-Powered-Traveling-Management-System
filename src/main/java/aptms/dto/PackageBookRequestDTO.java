package aptms.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PackageBookRequestDTO {

    @NotNull(message = "A start date is required")
    @FutureOrPresent(message = "The start date cannot be in the past")
    private LocalDate startDate;

    // Payment method/reference are no longer collected here — checkout now happens
    // via a real SSLCommerz redirect (see PaymentController), not a self-declared field.
}

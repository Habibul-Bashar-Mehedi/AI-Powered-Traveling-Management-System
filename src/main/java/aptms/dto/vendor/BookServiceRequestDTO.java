package aptms.dto.vendor;

import aptms.enums.PaymentMethod;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Payment reference (mobile/account number) is required")
    @Size(max = 100)
    private String paymentReference;
}

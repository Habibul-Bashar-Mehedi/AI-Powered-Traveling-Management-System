package aptms.dto;

import aptms.enums.SimulatedPaymentOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentSimulateRequestDTO {

    @NotBlank(message = "Transaction id is required")
    private String txId;

    @NotNull(message = "Outcome is required")
    private SimulatedPaymentOutcome outcome;
}

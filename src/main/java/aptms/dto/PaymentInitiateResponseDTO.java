package aptms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentInitiateResponseDTO {
    private String gatewayPageUrl;
    private String txId;
    private BigDecimal amount;
    private String currencyCode;
}

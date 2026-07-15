package aptms.services;

import aptms.dto.PaymentInitiateRequestDTO;
import aptms.dto.PaymentInitiateResponseDTO;
import aptms.dto.PaymentStatusDTO;
import aptms.enums.SimulatedPaymentOutcome;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    PaymentInitiateResponseDTO initiate(UUID userId, PaymentInitiateRequestDTO request);

    /** Returns "paid" | "failed" | "invalid" for the controller to build the redirect. */
    String handleSuccess(String txId, String valId);

    String handleFail(String txId);

    String handleCancel(String txId);

    void handleIpn(Map<String, String> params);

    PaymentStatusDTO getStatus(UUID userId, String txId);

    /**
     * Resolves a payment created against the app's own "simulated checkout" test
     * page — only ever reachable when SSLCommerz isn't configured. Returns
     * "paid" | "failed" | "cancelled".
     */
    String simulate(UUID userId, String txId, SimulatedPaymentOutcome outcome);

    /** Cancels bookings whose payment has sat PENDING past the configured expiry window. */
    void expireStalePayments();
}

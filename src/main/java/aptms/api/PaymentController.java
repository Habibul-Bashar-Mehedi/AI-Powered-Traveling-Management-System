package aptms.api;

import aptms.dto.PaymentInitiateRequestDTO;
import aptms.dto.PaymentInitiateResponseDTO;
import aptms.dto.PaymentSimulateRequestDTO;
import aptms.dto.PaymentStatusDTO;
import aptms.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer-facing payment endpoints. /initiate and /{txId} require a logged-in
 * user; /success, /fail, /cancel, /ipn are called directly by SSLCommerz (never
 * carries our JWT) and are permitted anonymously in SecurityConfig — trust comes
 * from server-to-server transaction validation, not from the caller's identity.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "SSLCommerz checkout initiation and callbacks")
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Start a SSLCommerz checkout session for a pending booking")
    public ResponseEntity<PaymentInitiateResponseDTO> initiate(@Valid @RequestBody PaymentInitiateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiate(getCurrentUserId(), request));
    }

    @PostMapping("/success")
    public ResponseEntity<Void> success(@RequestParam Map<String, String> params) {
        return redirectToResult(params.get("tran_id"), safely(() ->
                paymentService.handleSuccess(params.get("tran_id"), params.get("val_id"))));
    }

    @PostMapping("/fail")
    public ResponseEntity<Void> fail(@RequestParam Map<String, String> params) {
        return redirectToResult(params.get("tran_id"), safely(() -> paymentService.handleFail(params.get("tran_id"))));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(@RequestParam Map<String, String> params) {
        return redirectToResult(params.get("tran_id"), safely(() -> paymentService.handleCancel(params.get("tran_id"))));
    }

    @PostMapping("/simulate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Resolve a payment via the app's own simulated checkout page (test mode only, no SSLCommerz credentials configured)")
    public ResponseEntity<Map<String, String>> simulate(@Valid @RequestBody PaymentSimulateRequestDTO request) {
        String status = paymentService.simulate(getCurrentUserId(), request.getTxId(), request.getOutcome());
        return ResponseEntity.ok(Map.of("status", status));
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> ipn(@RequestParam Map<String, String> params) {
        log.info("SSLCommerz IPN received for tx {}", params.get("tran_id"));
        try {
            paymentService.handleIpn(params);
        } catch (Exception e) {
            // SSLCommerz retries IPN delivery on non-200 responses — always ack with 200 so
            // it doesn't hammer us, even if our own processing hit an unexpected error; the
            // browser-redirect handlers (success/fail/cancel) are the primary resolution path.
            log.error("IPN handling failed for tx {}", params.get("tran_id"), e);
        }
        return ResponseEntity.ok("OK");
    }

    /** Never let a callback handler's exception surface as a raw error page mid gateway-redirect. */
    private String safely(java.util.function.Supplier<String> action) {
        try {
            return action.get();
        } catch (Exception e) {
            log.error("Payment callback handling failed", e);
            return "error";
        }
    }

    @GetMapping("/{txId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Fetch the final status of a payment, for the result page")
    public ResponseEntity<PaymentStatusDTO> getStatus(@PathVariable String txId) {
        return ResponseEntity.ok(paymentService.getStatus(getCurrentUserId(), txId));
    }

    private ResponseEntity<Void> redirectToResult(String txId, String status) {
        String location = frontendUrl + "/payment/result?txId=" + (txId != null ? txId : "") + "&status=" + status;
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(Objects.requireNonNull(auth).getName());
    }
}

package aptms.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases capacity held by bookings whose checkout was started (reserving
 * inventory as PENDING/unpaid) but never completed — otherwise an abandoned
 * SSLCommerz redirect would lock that inventory indefinitely.
 * Runs every 5 minutes; the actual expiry window is app.payment.pending-expiry-minutes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingPaymentExpiryScheduler {

    private final PaymentService paymentService;

    @Scheduled(cron = "0 */5 * * * *")
    public void expireStalePendingPayments() {
        try {
            paymentService.expireStalePayments();
        } catch (Exception e) {
            log.error("PendingPaymentExpiryScheduler failed", e);
        }
    }
}

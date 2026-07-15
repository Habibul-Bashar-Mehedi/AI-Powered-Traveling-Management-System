package aptms.enums;

/**
 * Outcome chosen on the app's own "simulated checkout" test page — only reachable
 * when {@link aptms.services.SslcommerzClient#isConfigured()} is false, i.e. no real
 * gateway credentials exist yet. Lets the full paid/failed/cancelled flow be exercised
 * without a live SSLCommerz sandbox account.
 */
public enum SimulatedPaymentOutcome {
    PAID,
    FAILED,
    CANCELLED
}

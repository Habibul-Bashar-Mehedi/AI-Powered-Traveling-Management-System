package aptms.enums;

/**
 * User's preferred payment channel, selected in the "Pay Now" modal before
 * redirecting to SSLCommerz. Passed through as a filter hint (SSLCommerz's
 * {@code multi_card_name} parameter) so the hosted checkout page pre-narrows
 * to that category — the user still completes the actual payment on SSLCommerz's
 * page, this only skips them past the full method list.
 */
public enum PreferredPaymentMethod {
    MOBILE_BANK,
    CARD,
    INTERNET_BANK
}

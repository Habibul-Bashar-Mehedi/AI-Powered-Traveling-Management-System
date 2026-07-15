package aptms.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Thin wrapper around SSLCommerz's sandbox REST API — no official Java SDK exists,
 * so this makes plain form-encoded/GET calls via the app's existing RestTemplate bean,
 * the same approach already used for the Gemini integration (AiChatServiceImpl).
 *
 * Docs: https://developer.sslcommerz.com/doc/v4/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SslcommerzClient {

    private final RestTemplate restTemplate;

    @Value("${app.payment.sslcommerz.store-id}")
    private String storeId;

    @Value("${app.payment.sslcommerz.store-password}")
    private String storePassword;

    @Value("${app.payment.sslcommerz.api-url}")
    private String apiUrl;

    @Value("${app.payment.sslcommerz.validation-url}")
    private String validationUrl;

    public boolean isConfigured() {
        return storeId != null && !storeId.isBlank() && storePassword != null && !storePassword.isBlank();
    }

    public static class CustomerInfo {
        public final String name;
        public final String email;
        public final String phone;
        public final String address;

        public CustomerInfo(String name, String email, String phone, String address) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.address = address;
        }
    }

    /**
     * Initiates a checkout session for one txId/amount, returning the GatewayPageURL
     * the frontend should redirect the browser to.
     *
     * @param multiCardName optional SSLCommerz {@code multi_card_name} filter (e.g. "mobilebank",
     *                       "internetbank", "mastercard,visacard,amexcard,othercard") that pre-narrows
     *                       the hosted page to the user's chosen category — null/blank shows all methods.
     */
    public String initiateSession(String txId, BigDecimal amount, String productName,
                                   CustomerInfo customer, String successUrl, String failUrl,
                                   String cancelUrl, String ipnUrl, String multiCardName) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "SSLCommerz is not configured — set SSLCOMMERZ_STORE_ID/SSLCOMMERZ_STORE_PASSWORD to enable checkout.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("store_id", storeId);
        form.add("store_passwd", storePassword);
        form.add("total_amount", amount.toPlainString());
        form.add("currency", "BDT");
        form.add("tran_id", txId);
        form.add("success_url", successUrl);
        form.add("fail_url", failUrl);
        form.add("cancel_url", cancelUrl);
        form.add("ipn_url", ipnUrl);
        form.add("cus_name", customer.name);
        form.add("cus_email", customer.email);
        form.add("cus_phone", customer.phone);
        form.add("cus_add1", customer.address);
        form.add("cus_city", "Dhaka");
        form.add("cus_country", "Bangladesh");
        form.add("shipping_method", "NO");
        form.add("product_name", productName);
        form.add("product_category", "Travel");
        form.add("product_profile", "general");
        if (multiCardName != null && !multiCardName.isBlank()) {
            form.add("multi_card_name", multiCardName);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
        if (response == null) {
            throw new IllegalStateException("SSLCommerz returned no response for tx " + txId);
        }

        Object status = response.get("status");
        Object gatewayUrl = response.get("GatewayPageURL");
        if (!"SUCCESS".equals(status) || gatewayUrl == null) {
            log.warn("SSLCommerz initiate failed for tx {}: {}", txId, response);
            throw new IllegalStateException("Could not start checkout: " + response.getOrDefault("failedreason", "unknown error"));
        }

        return gatewayUrl.toString();
    }

    public static class ValidationResult {
        public final boolean valid;
        public final BigDecimal amount;
        public final String cardType;
        public final Map<String, Object> raw;

        public ValidationResult(boolean valid, BigDecimal amount, String cardType, Map<String, Object> raw) {
            this.valid = valid;
            this.amount = amount;
            this.cardType = cardType;
            this.raw = raw;
        }
    }

    /**
     * Re-validates a transaction server-to-server against SSLCommerz — never trust the
     * browser-redirect payload alone, always confirm with this call before marking anything PAID.
     */
    public ValidationResult validateTransaction(String valId) {
        if (!isConfigured()) {
            throw new IllegalStateException("SSLCommerz is not configured — cannot validate transaction.");
        }

        String url = UriComponentsBuilder.fromUriString(validationUrl)
                .queryParam("val_id", valId)
                .queryParam("store_id", storeId)
                .queryParam("store_passwd", storePassword)
                .queryParam("format", "json")
                .toUriString();

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            return new ValidationResult(false, null, null, Map.of());
        }

        Object status = response.get("status");
        boolean valid = "VALID".equals(status) || "VALIDATED".equals(status);
        BigDecimal amount = null;
        Object amountObj = response.get("amount");
        if (amountObj != null) {
            try {
                amount = new BigDecimal(amountObj.toString());
            } catch (NumberFormatException ignored) {
                // leave amount null — caller treats missing amount as unverifiable
            }
        }
        Object cardType = response.get("card_type");
        return new ValidationResult(valid, amount, cardType != null ? cardType.toString() : null, response);
    }
}

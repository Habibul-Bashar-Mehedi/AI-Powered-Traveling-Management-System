package aptms.services.impl;

import aptms.dto.PaymentInitiateRequestDTO;
import aptms.dto.PaymentInitiateResponseDTO;
import aptms.dto.PaymentStatusDTO;
import aptms.entities.PackageBooking;
import aptms.entities.Payment;
import aptms.entities.User;
import aptms.entities.VendorBooking;
import aptms.enums.CancelledBy;
import aptms.enums.PaymentBookingType;
import aptms.enums.PaymentMethod;
import aptms.enums.PaymentStatus;
import aptms.enums.PreferredPaymentMethod;
import aptms.enums.SimulatedPaymentOutcome;
import aptms.enums.VendorBookingStatus;
import aptms.enums.VendorPaymentStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.PackageBookingRepository;
import aptms.repositories.PaymentRepository;
import aptms.repositories.UserRepository;
import aptms.repositories.VendorBookingRepository;
import aptms.services.PaymentService;
import aptms.services.SslcommerzClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final VendorBookingRepository vendorBookingRepository;
    private final PackageBookingRepository packageBookingRepository;
    private final UserRepository userRepository;
    private final SslcommerzClient sslcommerzClient;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.payment.pending-expiry-minutes}")
    private int pendingExpiryMinutes;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                               VendorBookingRepository vendorBookingRepository,
                               PackageBookingRepository packageBookingRepository,
                               UserRepository userRepository,
                               SslcommerzClient sslcommerzClient) {
        this.paymentRepository = paymentRepository;
        this.vendorBookingRepository = vendorBookingRepository;
        this.packageBookingRepository = packageBookingRepository;
        this.userRepository = userRepository;
        this.sslcommerzClient = sslcommerzClient;
    }

    @Override
    @Transactional
    public PaymentInitiateResponseDTO initiate(UUID userId, PaymentInitiateRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdNotFoundException("User not found: " + userId));

        Payment payment = new Payment();
        payment.setTxId(generateTxId());

        BigDecimal amount;
        String productName;

        if (request.getBookingType() == PaymentBookingType.VENDOR_BOOKING) {
            VendorBooking booking = vendorBookingRepository.findByBookingIdAndUserId(request.getBookingId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getBookingId()));
            if (booking.getPaymentStatus() != VendorPaymentStatus.PENDING) {
                throw new IllegalStateException("This booking is not awaiting payment.");
            }
            amount = booking.getGrossAmount();
            productName = booking.getService().getServiceName();
            payment.setVendorBooking(booking);
        } else {
            PackageBooking pb = packageBookingRepository.findById(request.getBookingId())
                    .filter(p -> p.getUser().getId().equals(userId))
                    .orElseThrow(() -> new IllegalArgumentException("Package booking not found: " + request.getBookingId()));
            if (pb.getPaymentStatus() != VendorPaymentStatus.PENDING) {
                throw new IllegalStateException("This package booking is not awaiting payment.");
            }
            amount = pb.getTotalGrossAmount();
            productName = pb.getPackageEntity().getName();
            payment.setPackageBooking(pb);
        }

        payment.setAmount(amount);
        payment = paymentRepository.save(payment);

        String gatewayUrl;
        if (sslcommerzClient.isConfigured()) {
            String contactPhone = request.getContactPhone() != null && !request.getContactPhone().isBlank()
                    ? request.getContactPhone()
                    : "01700000000";
            SslcommerzClient.CustomerInfo customer = new SslcommerzClient.CustomerInfo(
                    user.getUsername(), user.getEmail(), contactPhone, "Dhaka, Bangladesh");

            gatewayUrl = sslcommerzClient.initiateSession(
                    payment.getTxId(),
                    amount,
                    productName,
                    customer,
                    baseUrl + "/api/payments/success",
                    baseUrl + "/api/payments/fail",
                    baseUrl + "/api/payments/cancel",
                    baseUrl + "/api/payments/ipn",
                    toMultiCardName(request.getPreferredMethod(), request.getPreferredProvider()));
        } else {
            // No real gateway credentials yet — route to the app's own clearly-labeled
            // "simulated checkout" test page instead of failing the whole flow. This
            // mirrors the vendor-payout simulation already used elsewhere in this app
            // when a real disbursement API isn't available.
            log.warn("SSLCommerz is not configured — routing tx {} to the simulated checkout page.", payment.getTxId());
            StringBuilder mockUrl = new StringBuilder(frontendUrl)
                    .append("/payment/mock-checkout?txId=").append(payment.getTxId());
            if (request.getPreferredMethod() != null) {
                mockUrl.append("&method=").append(request.getPreferredMethod());
            }
            if (request.getPreferredProvider() != null && !request.getPreferredProvider().isBlank()) {
                mockUrl.append("&provider=").append(request.getPreferredProvider().trim().toUpperCase());
            }
            gatewayUrl = mockUrl.toString();
        }

        PaymentInitiateResponseDTO dto = new PaymentInitiateResponseDTO();
        dto.setGatewayPageUrl(gatewayUrl);
        dto.setTxId(payment.getTxId());
        dto.setAmount(amount);
        dto.setCurrencyCode(payment.getCurrencyCode());
        return dto;
    }

    /**
     * Maps our user-facing choice to SSLCommerz's {@code multi_card_name} filter tokens.
     * Provider-specific codes are only applied when confirmed against SSLCommerz's v4
     * gateway parameter list (bkash, mastercard, visacard, amexcard) — Nagad/Rocket/Upay
     * don't have a documented individual token, so those fall back to the broader
     * "mobilebank" category rather than risk sending an incorrect value. Null means
     * "no preference" — SSLCommerz shows every method it supports.
     */
    private String toMultiCardName(PreferredPaymentMethod preferredMethod, String preferredProvider) {
        if (preferredProvider != null) {
            String provider = preferredProvider.trim().toUpperCase();
            switch (provider) {
                case "BKASH": return "bkash";
                case "VISA": return "visacard";
                case "MASTERCARD": return "mastercard";
                case "AMEX": return "amexcard";
                default: break; // unrecognized provider — fall through to category-level filter
            }
        }
        if (preferredMethod == null) return null;
        return switch (preferredMethod) {
            case MOBILE_BANK -> "mobilebank";
            case INTERNET_BANK -> "internetbank";
            case CARD -> "mastercard,visacard,amexcard,othercard";
        };
    }

    @Override
    @Transactional
    public String handleSuccess(String txId, String valId) {
        Payment payment = paymentRepository.findByTxId(txId).orElse(null);
        if (payment == null) return "invalid";
        if (payment.getStatus() == PaymentStatus.PAID) return "paid"; // idempotent — IPN/redirect may both land

        SslcommerzClient.ValidationResult result;
        try {
            result = sslcommerzClient.validateTransaction(valId);
        } catch (Exception e) {
            // Never let a gateway/config error escape as a raw error response to the
            // browser mid-redirect — resolve as a failed payment so the user still lands
            // on a coherent result page instead of a blank error screen.
            log.error("SSLCommerz validation call failed for tx {}", txId, e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            cancelLinkedBooking(payment, "Payment validation could not be completed: " + e.getMessage());
            return "failed";
        }

        payment.setGatewayValId(valId);
        payment.setGatewayResponse(String.valueOf(result.raw));

        boolean amountOk = result.amount != null && result.amount.compareTo(payment.getAmount()) >= 0;
        if (!result.valid || !amountOk) {
            log.warn("Payment validation failed for tx {}: valid={} amountOk={}", txId, result.valid, amountOk);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            cancelLinkedBooking(payment, "Payment validation failed");
            return "failed";
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setGatewayCardType(result.cardType);
        paymentRepository.save(payment);
        markLinkedBookingPaid(payment, result.cardType);
        return "paid";
    }

    @Override
    @Transactional
    public String handleFail(String txId) {
        return resolveNonPaid(txId, PaymentStatus.FAILED, "Payment failed at gateway");
    }

    @Override
    @Transactional
    public String handleCancel(String txId) {
        return resolveNonPaid(txId, PaymentStatus.CANCELLED, "Payment cancelled by user");
    }

    @Override
    @Transactional
    public void handleIpn(Map<String, String> params) {
        String txId = params.get("tran_id");
        if (txId == null) return;
        String status = params.get("status");
        String valId = params.get("val_id");

        if (valId != null && ("VALID".equalsIgnoreCase(status) || "VALIDATED".equalsIgnoreCase(status))) {
            handleSuccess(txId, valId);
        } else {
            resolveNonPaid(txId, PaymentStatus.FAILED, "IPN reported status: " + status);
        }
    }

    @Override
    @Transactional
    public String simulate(UUID userId, String txId, SimulatedPaymentOutcome outcome) {
        if (sslcommerzClient.isConfigured()) {
            throw new IllegalStateException("Simulated checkout is only available when SSLCommerz is not configured.");
        }

        Payment payment = paymentRepository.findByTxId(txId)
                .orElseThrow(() -> new IdNotFoundException("Payment not found: " + txId));
        if (!userId.equals(resolveOwnerId(payment))) {
            throw new IdNotFoundException("Payment not found: " + txId);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment.getStatus().name().toLowerCase(); // already resolved — idempotent
        }

        return switch (outcome) {
            case PAID -> {
                payment.setStatus(PaymentStatus.PAID);
                payment.setGatewayCardType("Simulated");
                payment.setGatewayResponse("Simulated success — SSLCommerz was not configured at the time of payment.");
                paymentRepository.save(payment);
                markLinkedBookingPaid(payment, "Simulated");
                yield "paid";
            }
            case CANCELLED -> resolveNonPaid(txId, PaymentStatus.CANCELLED,
                    "Simulated cancellation — SSLCommerz was not configured at the time of payment.");
            case FAILED -> resolveNonPaid(txId, PaymentStatus.FAILED,
                    "Simulated failure — SSLCommerz was not configured at the time of payment.");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentStatusDTO getStatus(UUID userId, String txId) {
        Payment payment = paymentRepository.findByTxId(txId)
                .orElseThrow(() -> new IdNotFoundException("Payment not found: " + txId));

        if (!userId.equals(resolveOwnerId(payment))) {
            throw new IdNotFoundException("Payment not found: " + txId);
        }

        PaymentStatusDTO dto = new PaymentStatusDTO();
        dto.setPaymentId(payment.getPaymentId());
        dto.setTxId(payment.getTxId());
        dto.setStatus(payment.getStatus());
        dto.setAmount(payment.getAmount());
        dto.setCurrencyCode(payment.getCurrencyCode());
        dto.setGatewayCardType(payment.getGatewayCardType());
        dto.setInitiatedAt(payment.getInitiatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());

        if (payment.getVendorBooking() != null) {
            dto.setBookingType(PaymentBookingType.VENDOR_BOOKING);
            dto.setBookingId(payment.getVendorBooking().getBookingId());
            dto.setBookingReference(payment.getVendorBooking().getService().getServiceName());
        } else if (payment.getPackageBooking() != null) {
            dto.setBookingType(PaymentBookingType.PACKAGE_BOOKING);
            dto.setBookingId(payment.getPackageBooking().getPackageBookingId());
            dto.setBookingReference(payment.getPackageBooking().getPackageEntity().getName());
        }
        return dto;
    }

    @Override
    @Transactional
    public void expireStalePayments() {
        Instant cutoff = Instant.now().minus(pendingExpiryMinutes, ChronoUnit.MINUTES);

        vendorBookingRepository.findStalePendingPayments(cutoff)
                .forEach(b -> cancelVendorBookingForUnpaid(b, "Payment not completed within " + pendingExpiryMinutes + " minutes"));

        packageBookingRepository.findStalePendingPayments(cutoff)
                .forEach(pb -> cancelPackageBookingForUnpaid(pb, "Payment not completed within " + pendingExpiryMinutes + " minutes"));
    }

    private UUID resolveOwnerId(Payment payment) {
        if (payment.getVendorBooking() != null) {
            return payment.getVendorBooking().getUser().getId();
        } else if (payment.getPackageBooking() != null) {
            return payment.getPackageBooking().getUser().getId();
        }
        return null;
    }

    private String resolveNonPaid(String txId, PaymentStatus status, String reason) {
        Payment payment = paymentRepository.findByTxId(txId).orElse(null);
        if (payment == null) return "invalid";
        if (payment.getStatus() == PaymentStatus.PAID) return "paid"; // already resolved successfully elsewhere

        payment.setStatus(status);
        paymentRepository.save(payment);
        cancelLinkedBooking(payment, reason);
        return status == PaymentStatus.FAILED ? "failed" : "cancelled";
    }

    private void markLinkedBookingPaid(Payment payment, String cardType) {
        PaymentMethod method = mapCardType(cardType);
        if (payment.getVendorBooking() != null) {
            VendorBooking b = payment.getVendorBooking();
            b.setPaymentStatus(VendorPaymentStatus.PAID);
            b.setPaymentMethod(method);
            b.setPaymentReference(payment.getGatewayValId());
            vendorBookingRepository.save(b);
        } else if (payment.getPackageBooking() != null) {
            PackageBooking pb = payment.getPackageBooking();
            pb.setPaymentStatus(VendorPaymentStatus.PAID);
            pb.setPaymentMethod(method);
            pb.setPaymentReference(payment.getGatewayValId());
            packageBookingRepository.save(pb);

            vendorBookingRepository.findByPackageBooking_PackageBookingId(pb.getPackageBookingId())
                    .forEach(c -> {
                        c.setPaymentStatus(VendorPaymentStatus.PAID);
                        c.setPaymentMethod(method);
                        c.setPaymentReference(payment.getGatewayValId());
                        vendorBookingRepository.save(c);
                    });
        }
    }

    private void cancelLinkedBooking(Payment payment, String reason) {
        if (payment.getVendorBooking() != null) {
            cancelVendorBookingForUnpaid(payment.getVendorBooking(), reason);
        } else if (payment.getPackageBooking() != null) {
            cancelPackageBookingForUnpaid(payment.getPackageBooking(), reason);
        }
    }

    private void cancelVendorBookingForUnpaid(VendorBooking booking, String reason) {
        if (booking.getBookingStatus() == VendorBookingStatus.CANCELLED
                || booking.getBookingStatus() == VendorBookingStatus.REJECTED
                || booking.getBookingStatus() == VendorBookingStatus.COMPLETED) {
            return; // already resolved — nothing to release
        }
        booking.setBookingStatus(VendorBookingStatus.CANCELLED);
        if (booking.getPaymentStatus() != VendorPaymentStatus.PAID) {
            booking.setPaymentStatus(VendorPaymentStatus.FAILED);
        }
        booking.setCancellationReason(reason);
        booking.setCancelledBy(CancelledBy.SYSTEM);
        vendorBookingRepository.save(booking);
        log.info("Booking {} cancelled by system: {}", booking.getBookingId(), reason);
    }

    private void cancelPackageBookingForUnpaid(PackageBooking pb, String reason) {
        if (pb.getPaymentStatus() != VendorPaymentStatus.PAID) {
            pb.setPaymentStatus(VendorPaymentStatus.FAILED);
            packageBookingRepository.save(pb);
        }
        vendorBookingRepository.findByPackageBooking_PackageBookingId(pb.getPackageBookingId())
                .forEach(c -> cancelVendorBookingForUnpaid(c, reason));
    }

    private PaymentMethod mapCardType(String cardType) {
        if (cardType == null) return PaymentMethod.CARD;
        String lower = cardType.toLowerCase();
        if (lower.contains("bkash")) return PaymentMethod.BKASH;
        if (lower.contains("rocket")) return PaymentMethod.ROCKET;
        if (lower.contains("nagad")) return PaymentMethod.NAGAD;
        if (lower.contains("bank")) return PaymentMethod.BANK_TRANSFER;
        return PaymentMethod.CARD;
    }

    private String generateTxId() {
        return "SMTS" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

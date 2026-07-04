package aptms.services;

import aptms.entities.Vendor;
import aptms.entities.VendorBooking;
import aptms.entities.WalletTransaction;
import aptms.enums.TransactionType;
import aptms.enums.VendorBookingStatus;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled component responsible for:
 *  1. Automatically marking CONFIRMED bookings as COMPLETED once their end-date has passed.
 *  2. Settling wallet earnings: moving vendor earnings from pendingBalance → walletBalance
 *     exactly 48 hours after completion (BRD FR-WAL-007).
 *
 * Runs every hour.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCompletionScheduler {

    private final VendorBookingRepository bookingRepository;
    private final VendorRepository vendorRepository;
    private final WalletTransactionRepository transactionRepository;

    /**
     * Marks CONFIRMED bookings as COMPLETED when their end_date has passed.
     * Runs at the top of every hour.
     *
     * Requirements: BRD FR-BKG (post-service completion), FR-WAL-007
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void completeExpiredBookings() {
        List<VendorBooking> toComplete = bookingRepository.findBookingsToComplete();
        if (toComplete.isEmpty()) return;

        log.info("BookingCompletionScheduler: completing {} bookings", toComplete.size());

        for (VendorBooking booking : toComplete) {
            booking.setBookingStatus(VendorBookingStatus.COMPLETED);
            booking.setCompletedAt(Instant.now());
            bookingRepository.save(booking);
            log.debug("Booking {} marked as COMPLETED", booking.getBookingId());
        }
    }

    /**
     * Settles earnings from pendingBalance → walletBalance (available)
     * for bookings completed more than 48 hours ago (BRD FR-WAL-007).
     * Runs every 30 minutes.
     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void settleWalletEarnings() {
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);

        List<VendorBooking> settleableBookings = bookingRepository.findSettleableBookings(cutoff).stream()
                .filter(b -> !hasWalletCreditForBooking(b))
                .toList();

        if (settleableBookings.isEmpty()) return;

        log.info("BookingCompletionScheduler: settling earnings for {} bookings", settleableBookings.size());

        for (VendorBooking booking : settleableBookings) {
            Vendor vendor = booking.getVendor();
            BigDecimal netAmount = booking.getNetAmount();

            // Move from pending to available
            vendor.setPendingBalance(
                    vendor.getPendingBalance().subtract(netAmount).max(BigDecimal.ZERO));
            vendor.setWalletBalance(vendor.getWalletBalance().add(netAmount));
            vendorRepository.save(vendor);

            // Record wallet transaction
            WalletTransaction tx = new WalletTransaction();
            tx.setVendor(vendor);
            tx.setBooking(booking);
            tx.setTransactionType(TransactionType.CREDIT);
            tx.setAmount(netAmount);
            tx.setBalanceAfter(vendor.getWalletBalance());
            tx.setDescription("Earnings settled for booking " + booking.getBookingId());
            transactionRepository.save(tx);

            log.debug("Settled {} for booking {} to vendor {}", netAmount, booking.getBookingId(), vendor.getVendorId());
        }
    }

    /**
     * Checks whether a CREDIT wallet transaction already exists for the given booking
     * to prevent double-settling.
     */
    private boolean hasWalletCreditForBooking(VendorBooking booking) {
        return transactionRepository
                .existsByBooking_BookingIdAndTransactionType(booking.getBookingId(), TransactionType.CREDIT);
    }
}


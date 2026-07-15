package aptms.services;

import java.util.UUID;

public interface ReceiptService {

    /**
     * Generates a PDF receipt for the given booking, scoped to the requesting user.
     *
     * @param bookingId the {@code Booking} id (numeric) or {@code VendorBooking} id (UUID), as a string
     * @param userId    the JWT-derived id of the requesting user — never trust a client-supplied user id
     * @return the rendered PDF as bytes
     * @throws IllegalArgumentException if no booking with that id exists for this user
     * @throws IllegalStateException    if the booking exists but isn't in a receipt-eligible state
     */
    byte[] generateReceipt(String bookingId, UUID userId);
}

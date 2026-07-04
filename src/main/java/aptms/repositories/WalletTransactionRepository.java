package aptms.repositories;

import aptms.entities.WalletTransaction;
import aptms.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findByVendorVendorIdOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);

    boolean existsByBooking_BookingIdAndTransactionType(UUID bookingId, TransactionType transactionType);

    @Modifying
    @Query("DELETE FROM WalletTransaction wt WHERE wt.vendor.vendorId = :vendorId")
    void deleteByVendorId(@Param("vendorId") UUID vendorId);
}


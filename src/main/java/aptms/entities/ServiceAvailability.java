package aptms.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ServiceAvailability entity managing per-date availability and pricing.
 * Requirements: BRD FR-SVC-003 — Availability Calendar
 */
@Entity
@Table(name = "service_availability")
@Data
public class ServiceAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID availabilityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private VendorService service;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(name = "total_slots", nullable = false)
    private Integer totalSlots;

    @Column(name = "booked_slots", nullable = false)
    private Integer bookedSlots = 0;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Column(name = "override_price", precision = 12, scale = 2)
    private BigDecimal overridePrice;

    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;
}


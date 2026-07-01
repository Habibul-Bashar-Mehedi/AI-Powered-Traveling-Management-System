package aptms.services.impl;

import aptms.dto.vendor.AnalyticsSummaryDTO;
import aptms.entities.Vendor;
import aptms.enums.ServiceStatus;
import aptms.enums.VendorBookingStatus;
import aptms.exceptions.IdNotFoundException;
import aptms.repositories.VendorBookingRepository;
import aptms.repositories.VendorRepository;
import aptms.repositories.VendorServiceRepository;
import aptms.services.VendorAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorAnalyticsServiceImpl implements VendorAnalyticsService {

    private final VendorRepository vendorRepository;
    private final VendorBookingRepository bookingRepository;
    private final VendorServiceRepository serviceRepository;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsSummaryDTO getSummary(UUID userId, LocalDate from, LocalDate to) {
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new IdNotFoundException("Vendor profile not found for user: " + userId));

        UUID vendorId = vendor.getVendorId();

        // Always anchor period-specific revenue to the current calendar periods
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant dayStart  = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nowInstant = Instant.now();

        // For the user-selected range use the provided params (or defaults)
        Instant fromInstant = from != null ? from.atStartOfDay().toInstant(ZoneOffset.UTC) : Instant.EPOCH;
        Instant toInstant   = to   != null ? to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : nowInstant;

        // Query the wider of (monthStart, fromInstant) so that today/week/month
        // calculations can be derived from the same result set when possible.
        Instant extendedFrom = monthStart.isBefore(fromInstant) ? monthStart : fromInstant;
        // For period-specific revenue always extend the upper bound to now
        Instant extendedTo = toInstant.isBefore(nowInstant) ? nowInstant : toInstant;
        var allBookings = bookingRepository.findByVendorVendorIdAndCreatedAtBetween(vendorId, extendedFrom, extendedTo);

        // Bookings that fall within the user-requested range
        var rangeBookings = allBookings.stream()
                .filter(b -> !b.getCreatedAt().isBefore(fromInstant) && b.getCreatedAt().isBefore(toInstant))
                .collect(Collectors.toList());

        long totalBookings = rangeBookings.size();
        long confirmed = rangeBookings.stream().filter(b -> b.getBookingStatus() == VendorBookingStatus.CONFIRMED || b.getBookingStatus() == VendorBookingStatus.COMPLETED).count();
        long cancelled = rangeBookings.stream().filter(b -> b.getBookingStatus() == VendorBookingStatus.CANCELLED || b.getBookingStatus() == VendorBookingStatus.REJECTED).count();

        BigDecimal totalRevenue = rangeBookings.stream()
                .filter(b -> b.getBookingStatus() == VendorBookingStatus.COMPLETED)
                .map(b -> b.getNetAmount() != null ? b.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double cancellationRate = totalBookings > 0
                ? (double) cancelled / totalBookings * 100.0
                : 0.0;

        long activeServices = serviceRepository.countByVendorVendorIdAndStatus(vendorId, ServiceStatus.ACTIVE);

        // Build revenue time series (daily) for the user-selected range
        Map<String, BigDecimal> timeSeries = new LinkedHashMap<>();
        rangeBookings.stream()
                .filter(b -> b.getBookingStatus() == VendorBookingStatus.COMPLETED)
                .forEach(b -> {
                    String day = b.getCreatedAt().atOffset(ZoneOffset.UTC).toLocalDate().toString();
                    timeSeries.merge(day, b.getNetAmount() != null ? b.getNetAmount() : BigDecimal.ZERO, BigDecimal::add);
                });

        // Top services within the user-selected range
        Map<UUID, List<aptms.entities.VendorBooking>> byService = rangeBookings.stream()
                .filter(b -> b.getService() != null && b.getBookingStatus() == VendorBookingStatus.COMPLETED)
                .collect(Collectors.groupingBy(b -> b.getService().getServiceId()));

        List<AnalyticsSummaryDTO.ServicePerformanceDTO> topServices = byService.entrySet().stream()
                .map(e -> {
                    AnalyticsSummaryDTO.ServicePerformanceDTO sp = new AnalyticsSummaryDTO.ServicePerformanceDTO();
                    sp.setServiceId(e.getKey().toString());
                    sp.setServiceName(e.getValue().get(0).getService().getServiceName());
                    sp.setBookingCount((long) e.getValue().size());
                    sp.setRevenue(e.getValue().stream()
                            .map(b -> b.getNetAmount() != null ? b.getNetAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    return sp;
                })
                .sorted((a, b) -> b.getRevenue().compareTo(a.getRevenue()))
                .limit(5)
                .collect(Collectors.toList());

        AnalyticsSummaryDTO dto = new AnalyticsSummaryDTO();
        dto.setTotalRevenue(totalRevenue);
        dto.setRevenueTimeSeries(timeSeries);
        dto.setTotalBookings(totalBookings);
        dto.setConfirmedBookings(confirmed);
        dto.setCancelledBookings(cancelled);
        dto.setCancellationRate(BigDecimal.valueOf(cancellationRate).setScale(2, RoundingMode.HALF_UP).doubleValue());
        dto.setActiveServices(activeServices);
        dto.setAverageRating(vendor.getAverageRating() != null ? vendor.getAverageRating().doubleValue() : 0.0);
        dto.setTotalReviews(vendor.getTotalReviews() != null ? vendor.getTotalReviews().longValue() : 0L);
        dto.setTopServices(topServices);

        // Period-specific revenue — always relative to current calendar periods
        dto.setRevenueThisMonth(sumCompletedRevenue(allBookings, monthStart, nowInstant));
        dto.setRevenueThisWeek(sumCompletedRevenue(allBookings, weekStart, nowInstant));
        dto.setRevenueToday(sumCompletedRevenue(allBookings, dayStart, nowInstant));
        return dto;
    }

    private BigDecimal sumCompletedRevenue(
            List<aptms.entities.VendorBooking> bookings, Instant from, Instant to) {
        return bookings.stream()
                .filter(b -> b.getBookingStatus() == VendorBookingStatus.COMPLETED
                        && !b.getCreatedAt().isBefore(from)
                        && b.getCreatedAt().isBefore(to))
                .map(b -> b.getNetAmount() != null ? b.getNetAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}


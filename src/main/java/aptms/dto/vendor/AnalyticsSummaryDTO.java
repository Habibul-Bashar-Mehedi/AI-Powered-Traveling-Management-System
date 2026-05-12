package aptms.dto.vendor;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for vendor analytics summary.
 * Requirements: BRD §4.5 — Analytics & Reporting
 */
@Data
public class AnalyticsSummaryDTO {

    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    private BigDecimal revenueThisWeek;
    private BigDecimal revenueToday;

    private Long totalBookings;
    private Long confirmedBookings;
    private Long cancelledBookings;
    private Double cancellationRate;

    private Long activeServices;
    private Double averageRating;
    private Long totalReviews;

    // Time series: date → revenue
    private Map<String, BigDecimal> revenueTimeSeries;

    // Top services by revenue
    private List<ServicePerformanceDTO> topServices;

    @Data
    public static class ServicePerformanceDTO {
        private String serviceId;
        private String serviceName;
        private BigDecimal revenue;
        private Long bookingCount;
        private Double averageRating;
    }
}


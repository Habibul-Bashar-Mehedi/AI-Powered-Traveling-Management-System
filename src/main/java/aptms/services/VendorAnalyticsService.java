package aptms.services;

import aptms.dto.vendor.AnalyticsSummaryDTO;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface for vendor analytics and reporting.
 */
public interface VendorAnalyticsService {

    AnalyticsSummaryDTO getSummary(UUID userId, LocalDate from, LocalDate to);
}


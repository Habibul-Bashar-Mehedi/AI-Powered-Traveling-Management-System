package aptms.repositories.projections;

public interface VendorAnalyticsProjection {
    Long getTotalVendors();
    Long getPendingVendors();
    Long getApprovedVendors();
    Long getRejectedVendors();
    Long getSuspendedVendors();
}

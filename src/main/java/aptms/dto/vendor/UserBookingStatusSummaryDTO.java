package aptms.dto.vendor;

import aptms.enums.VendorBookingStatus;
import lombok.Data;

import java.util.EnumMap;
import java.util.Map;

@Data
public class UserBookingStatusSummaryDTO {
    private long total;
    private Map<VendorBookingStatus, Long> counts = new EnumMap<>(VendorBookingStatus.class);
}

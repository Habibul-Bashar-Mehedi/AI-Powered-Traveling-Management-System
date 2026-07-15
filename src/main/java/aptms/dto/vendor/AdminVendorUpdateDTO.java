package aptms.dto.vendor;

import aptms.enums.VendorType;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminVendorUpdateDTO {

    private VendorType vendorType;

    @Size(max = 255)
    private String businessName;
}

package aptms.dto.vendor;

import aptms.enums.UserServiceRequestType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserServiceRequestDTO {

    @NotNull
    private UserServiceRequestType requestType;

    @Size(max = 255)
    private String title;

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    private Integer quantity = 1;

    @Size(max = 1000)
    private String specialRequests;
}

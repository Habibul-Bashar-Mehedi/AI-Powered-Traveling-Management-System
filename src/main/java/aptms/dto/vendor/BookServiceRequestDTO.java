package aptms.dto.vendor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookServiceRequestDTO {

    private LocalDate startDate;

    private LocalDate endDate;

    @Min(1)
    private Integer quantity = 1;

    @Size(max = 1000)
    private String specialRequests;
}

package aptms.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewReinstatementRequestDTO {

    /** APPROVE or REJECT */
    @NotBlank
    private String decision;

    @Size(max = 2000)
    private String reason;
}

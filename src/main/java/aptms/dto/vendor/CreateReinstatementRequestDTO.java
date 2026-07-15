package aptms.dto.vendor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReinstatementRequestDTO {

    @NotBlank
    @Size(min = 10, max = 2000)
    private String message;
}

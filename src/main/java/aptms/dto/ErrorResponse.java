package aptms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for error responses.
 * 
 * Requirements: 4.3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response structure for all API errors")
public class ErrorResponse {
    
    @Schema(
        description = "Error code identifying the type of error",
        example = "TOKEN_EXPIRED",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String error;
    
    @Schema(
        description = "Human-readable error message",
        example = "Token has expired",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;
    
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2025-06-01T12:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    @Schema(
        description = "API endpoint path where the error occurred",
        example = "/api/auth/login",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String path;
}

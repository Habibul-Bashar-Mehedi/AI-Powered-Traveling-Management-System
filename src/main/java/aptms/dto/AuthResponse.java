package aptms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for authentication responses containing tokens and user information.
 * 
 * Requirements: 4.3.2, FR-REG-002, FR-LGN-001
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private UserDTO user;
    
    private String accessToken;
    
    private String refreshToken;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private long expiresIn;  // seconds
}

package aptms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for user information in authentication responses.
 * 
 * Requirements: 4.3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private UUID id;
    
    private String username;
    
    private String email;
    
    private List<String> roles;
    
    private Instant createdAt;
    
    private Instant lastLoginAt;
}

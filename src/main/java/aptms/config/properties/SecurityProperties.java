package aptms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Password password = new Password();
    private Account account = new Account();

    @Data
    public static class Jwt {
        private String secret;
        
        // Legacy property (deprecated, use accessTokenTtl instead)
        private long expirationMs = 900000; // 15 minutes
        
        // New JWT properties for token management
        private long accessTokenTtl = 900000; // 15 minutes in milliseconds
        private long refreshTokenTtl = 604800000; // 7 days in milliseconds
        private String issuer = "com.aptms.auth";
        private String audience = "com.aptms.api";
        private String algorithm = "HS256";
    }

    @Data
    public static class Cors {
        private String[] allowedOrigins = {"http://localhost:4200"};
        private String[] allowedMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
        private String[] allowedHeaders = {"*"};
        private boolean allowCredentials = true;
    }

    @Data
    public static class Password {
        private int strength = 10; // BCrypt strength
        private int minLength = 8;
    }

    @Data
    public static class Account {
        private int maxFailedAttempts = 5;
        private int lockoutDurationMinutes = 15;
    }
}

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

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000; // 24 hours
        private String issuer = "APTMS";
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
}

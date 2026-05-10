package aptms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.database")
@Data
public class DatabaseProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    private Jpa jpa = new Jpa();

    @Data
    public static class Jpa {
        private String ddlAuto = "update";
        private boolean showSql = true;
        private String dialect;
    }
}

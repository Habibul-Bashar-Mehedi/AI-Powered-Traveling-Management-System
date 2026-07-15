package aptms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationProperties {
    private String name = "Smart Travel Management System";
    private String version = "1.0.0";
    private String apiVersion = "v1";
    private String baseUrl = "http://localhost:8080";
    private Pagination pagination = new Pagination();
    private Api api = new Api();

    @Data
    public static class Pagination {
        private int defaultPageSize = 20;
        private int maxPageSize = 100;
    }

    @Data
    public static class Api {
        private String basePath = "/api";
        private String version = "v1";
    }
}

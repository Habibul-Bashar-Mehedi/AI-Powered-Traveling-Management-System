package aptms.config;

import aptms.config.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private final SecurityProperties securityProperties;

    public CorsConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> configuredOrigins = Arrays.asList(securityProperties.getCors().getAllowedOrigins());
        List<String> allowedOriginPatterns = new ArrayList<>(configuredOrigins);
        if (!allowedOriginPatterns.contains("http://localhost:*")) {
            allowedOriginPatterns.add("http://localhost:*");
        }
        if (!allowedOriginPatterns.contains("http://127.0.0.1:*")) {
            allowedOriginPatterns.add("http://127.0.0.1:*");
        }

        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(
            Arrays.asList(securityProperties.getCors().getAllowedMethods())
        );
        configuration.setAllowedHeaders(
            Arrays.asList(securityProperties.getCors().getAllowedHeaders())
        );
        configuration.setAllowCredentials(
            securityProperties.getCors().isAllowCredentials()
        );
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

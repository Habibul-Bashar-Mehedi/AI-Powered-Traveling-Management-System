package aptms.config;

import aptms.config.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    private final SecurityProperties securityProperties;

    public CorsConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOrigins(
            Arrays.asList(securityProperties.getCors().getAllowedOrigins())
        );
        configuration.setAllowedMethods(
            Arrays.asList(securityProperties.getCors().getAllowedMethods())
        );
        configuration.setAllowedHeaders(
            Arrays.asList(securityProperties.getCors().getAllowedHeaders())
        );
        configuration.setAllowCredentials(
            securityProperties.getCors().isAllowCredentials()
        );
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

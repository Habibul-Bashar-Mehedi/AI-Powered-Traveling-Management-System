package aptms.config;

import aptms.config.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;

    public CorsConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(securityProperties.getCors().getAllowedOrigins())
                .allowedMethods(securityProperties.getCors().getAllowedMethods())
                .allowedHeaders(securityProperties.getCors().getAllowedHeaders())
                .allowCredentials(securityProperties.getCors().isAllowCredentials())
                .maxAge(3600);
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

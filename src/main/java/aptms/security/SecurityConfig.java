package aptms.security;

import aptms.services.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;

/**
 * Spring Security configuration for JWT authentication.
 * 
 * Configures:
 * - Stateless session management (no server-side sessions) when JWT is enabled
 * - JWT authentication filter before UsernamePasswordAuthenticationFilter (conditional)
 * - CORS configuration for frontend origin
 * - Public endpoints (login, register, refresh)
 * - Authentication required for all other endpoints
 * - Exception handling for 401/403 responses
 * - Feature flag support for gradual rollout
 * 
 * Requirements: BR-2, BR-5, 2.2.1
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final FeatureFlagService featureFlagService;
    
    /**
     * Configure security filter chain with JWT authentication.
     * 
     * Filter order (when JWT is enabled):
     * 1. CorsFilter (handled by Spring Security)
     * 2. JwtAuthenticationFilter (custom) - only if JWT_ENABLED=true
     * 3. UsernamePasswordAuthenticationFilter (disabled when JWT enabled)
     * 4. AuthorizationFilter
     * 5. ExceptionTranslationFilter
     * 
     * When JWT is disabled (JWT_ENABLED=false):
     * - Session-based authentication is used
     * - JWT filter is not added to the chain
     * - Existing session-based endpoints continue working
     * 
     * @param http HttpSecurity configuration
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean jwtEnabled = featureFlagService.isJwtEnabled();
        log.info("Configuring Spring Security - JWT authentication enabled: {}", jwtEnabled);
        
        http
            // Disable CSRF (not needed for stateless JWT authentication)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Disable form login (prevents 302 redirects to /login)
            .formLogin(AbstractHttpConfigurer::disable)
            
            // Disable HTTP Basic (not needed for REST API with JWT)
            .httpBasic(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Configure session management (stateless when JWT enabled, stateful otherwise)
            .sessionManagement(session -> {
                if (jwtEnabled) {
                    log.info("Configuring stateless session management (JWT mode)");
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                } else {
                    log.info("Configuring stateful session management (session-based mode)");
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                }
            })
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Always allow preflight requests FIRST
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public endpoints (no authentication required) - MUST be before anyRequest()
                // AuthController is mapped to /api/auth — these are the real paths.
                // Using both exact paths and wildcard patterns for maximum compatibility
                .requestMatchers(
                    // Actual controller paths (AuthController @RequestMapping("/api/auth"))
                    "/api/auth/**",
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/refresh",
                    // Legacy / alternate prefixes kept for safety
                    "/api/v1/auth/**",
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/auth/refresh",
                    "/auth/**",
                    "/auth/login",
                    "/auth/register",
                    "/auth/refresh",
                    // Misc public paths
                    "/actuator/**",
                    "/error"
                ).permitAll()
                
                // Vendor registration requires any authenticated user (not yet VENDOR role)
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/vendor/register").authenticated()
                .requestMatchers("/api/v1/vendor/**").hasRole("VENDOR")
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Configure exception handling
            .exceptionHandling(exception -> exception
                // Handle authentication errors (401)
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("Authentication failed for request to: {} - {}", 
                        request.getRequestURI(), authException.getMessage());
                    
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    String jsonResponse = String.format(
                        "{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\",\"timestamp\":\"%s\",\"status\":%d,\"path\":\"%s\"}",
                        Instant.now().toString(),
                        HttpStatus.UNAUTHORIZED.value(),
                        request.getRequestURI()
                    );
                    
                    response.getWriter().write(jsonResponse);
                })
                
                // Handle authorization errors (403)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("Access denied for request to: {} - {}", 
                        request.getRequestURI(), accessDeniedException.getMessage());
                    
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    
                    String jsonResponse = String.format(
                        "{\"error\":\"FORBIDDEN\",\"message\":\"Access denied\",\"timestamp\":\"%s\",\"status\":%d,\"path\":\"%s\"}",
                        Instant.now().toString(),
                        HttpStatus.FORBIDDEN.value(),
                        request.getRequestURI()
                    );
                    
                    response.getWriter().write(jsonResponse);
                })
            );
        
        // Add JWT authentication filter only if JWT is enabled
        if (jwtEnabled) {
            log.info("Adding JWT authentication filter to security chain");
            http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        } else {
            log.info("JWT authentication disabled - using session-based authentication");
        }
        
        log.info("Spring Security configuration completed");
        return http.build();
    }
    
    /**
     * Password encoder bean using BCrypt.
     * 
     * BCrypt is a strong, adaptive hashing algorithm designed for password storage.
     * Strength factor of 10 provides good security while maintaining reasonable performance.
     * 
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        log.info("Creating BCryptPasswordEncoder with strength 10");
        return new BCryptPasswordEncoder(10);
    }
    
    /**
     * Authentication manager bean.
     * 
     * Required for authenticating users during login.
     * 
     * @param authenticationConfiguration Spring's authentication configuration
     * @return AuthenticationManager instance
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}

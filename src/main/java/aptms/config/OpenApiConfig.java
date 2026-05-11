package aptms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for JWT Authentication API documentation.
 * 
 * Provides comprehensive API documentation for all authentication endpoints
 * including request/response examples, error codes, and security requirements.
 * 
 * Requirements: NFR-5 (Maintainability)
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI-Powered Travel Management System - Authentication API")
                .version("1.0.0")
                .description("""
                    # JWT Authentication API
                    
                    This API provides stateless JWT-based authentication for the AI-Powered Travel Management System.
                    
                    ## Features
                    - User registration with JWT token issuance
                    - User login with JWT token issuance
                    - Token refresh with automatic rotation
                    - Single session logout
                    - Multi-device logout
                    - Current user profile retrieval
                    
                    ## Authentication
                    Most endpoints require a valid JWT access token in the Authorization header:
                    ```
                    Authorization: Bearer <access_token>
                    ```
                    
                    ## Token Lifecycle
                    - **Access Token**: 15 minutes TTL (900 seconds)
                    - **Refresh Token**: 7 days TTL (604800 seconds)
                    - Tokens are automatically rotated on refresh
                    
                    ## Error Codes
                    - `TOKEN_MISSING`: Missing Authorization header
                    - `TOKEN_INVALID`: Invalid token signature or malformed JWT
                    - `TOKEN_EXPIRED`: Token has expired
                    - `TOKEN_REVOKED`: Token is blacklisted
                    - `REFRESH_TOKEN_REUSE_DETECTED`: Refresh token reuse detected (security event)
                    
                    ## Security
                    - All tokens are cryptographically signed using HS256
                    - Passwords are hashed using BCrypt
                    - Refresh tokens are rotated on each use
                    - Token reuse detection with automatic session revocation
                    - Account lockout after 5 failed login attempts
                    """)
                .contact(new Contact()
                    .name("APTMS Development Team")
                    .email("support@aptms.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://aptms.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development Server"),
                new Server()
                    .url("https://api.aptms.com")
                    .description("Production Server")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter JWT access token obtained from /auth/login or /auth/register")));
    }
}

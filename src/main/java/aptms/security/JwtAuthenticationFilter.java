package aptms.security;

import aptms.services.AuthenticationEventLogger;
import aptms.services.JwtService;
import aptms.services.SecurityMetricsService;
import aptms.services.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * JWT Authentication Filter for validating JWT tokens on incoming requests.
 * 
 * This filter:
 * 1. Extracts Bearer token from Authorization header
 * 2. Validates token using JwtService
 * 3. Checks token is not blacklisted using TokenService
 * 4. Sets SecurityContext with authenticated user
 * 5. Handles token errors with appropriate HTTP responses
 * 6. Logs all token validation failures
 * 7. Records security metrics
 * 
 * Requirements: FR-MID-001, FR-MID-002, FR-MID-003, FR-MID-004, FR-MID-006, 3.1.3
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEventLogger eventLogger;
    private final SecurityMetricsService metricsService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        // Skip authentication for public endpoints
        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            log.debug("Skipping JWT authentication for public endpoint: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract token from Authorization header
            String token = extractToken(request);
            
            if (token == null) {
                log.debug("No JWT token found in request to: {}", requestPath);
                filterChain.doFilter(request, response);
                return;
            }
            
            log.debug("JWT token found, validating...");
            
            // Validate token
            if (!jwtService.validateToken(token)) {
                log.warn("Invalid JWT token");
                eventLogger.logTokenValidationFailure(
                    getClientIpAddress(request), 
                    getUserAgent(request), 
                    "TOKEN_INVALID", 
                    requestPath
                );
                metricsService.recordTokenValidationFailure("TOKEN_INVALID");
                sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Invalid or malformed token");
                return;
            }
            
            // Extract jti and check blacklist
            String jti = jwtService.extractClaims(token).getId();
            if (tokenService.isTokenBlacklisted(jti)) {
                log.warn("Token is blacklisted: {}", jti);
                eventLogger.logTokenValidationFailure(
                    getClientIpAddress(request), 
                    getUserAgent(request), 
                    "TOKEN_REVOKED", 
                    requestPath
                );
                metricsService.recordTokenValidationFailure("TOKEN_REVOKED");
                sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", "Token has been revoked");
                return;
            }
            
            // Extract user ID and load user details
            UUID userId = jwtService.extractUserId(token);
            log.debug("Token validated for user: {}", userId);
            
            // Check if user is already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());
                
                // Create authentication token
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("User authenticated: {} with roles: {}", userId, userDetails.getAuthorities());
            }
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            eventLogger.logTokenValidationFailure(
                getClientIpAddress(request), 
                getUserAgent(request), 
                "TOKEN_EXPIRED", 
                requestPath
            );
            metricsService.recordTokenValidationFailure("TOKEN_EXPIRED");
            sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token has expired");
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            eventLogger.logTokenValidationFailure(
                getClientIpAddress(request), 
                getUserAgent(request), 
                "TOKEN_INVALID", 
                requestPath
            );
            metricsService.recordTokenValidationFailure("TOKEN_INVALID");
            sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Invalid or malformed token");
        } catch (Exception e) {
            log.error("Error processing JWT authentication", e);
            eventLogger.logTokenValidationFailure(
                getClientIpAddress(request), 
                getUserAgent(request), 
                "TOKEN_INVALID", 
                requestPath
            );
            metricsService.recordTokenValidationFailure("TOKEN_INVALID");
            sendError(response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Authentication failed");
        }
    }
    
    /**
     * Extract Bearer token from Authorization header.
     * 
     * @param request HTTP request
     * @return JWT token string or null if not found
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * Check if the request path is a public endpoint that doesn't require authentication.
     *
     * Matches all known auth endpoint prefixes used by the AuthController
     * (mapped to /api/auth). Paths intentionally covered:
     *   /api/auth/login, /api/auth/register, /api/auth/refresh
     *   Legacy variations kept for safety.
     *
     * @param path Request path
     * @return true if public endpoint
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login")     ||
               path.startsWith("/api/auth/register")  ||
               path.startsWith("/api/auth/refresh")   ||
               // Legacy / alternate prefixes
               path.startsWith("/api/v1/auth/login")  ||
               path.startsWith("/api/v1/auth/register") ||
               path.startsWith("/api/v1/auth/refresh") ||
               path.startsWith("/auth/login")         ||
               path.startsWith("/auth/register")      ||
               path.startsWith("/auth/refresh")       ||
               path.startsWith("/actuator")           ||
               path.startsWith("/error");
    }
    
    /**
     * Get client IP address from request.
     * 
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
    
    /**
     * Get user agent from request.
     * 
     * @param request HTTP request
     * @return User agent string
     */
    private String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
    
    /**
     * Send JSON error response.
     * 
     * @param response HTTP response
     * @param status HTTP status
     * @param errorCode Error code
     * @param message Error message
     * @throws IOException if writing response fails
     */
    private void sendError(
            HttpServletResponse response,
            HttpStatus status,
            String errorCode,
            String message) throws IOException {
        
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String jsonResponse = String.format(
            "{\"error\":\"%s\",\"message\":\"%s\",\"timestamp\":\"%s\",\"status\":%d}",
            errorCode,
            message,
            Instant.now().toString(),
            status.value()
        );
        
        response.getWriter().write(jsonResponse);
    }
}

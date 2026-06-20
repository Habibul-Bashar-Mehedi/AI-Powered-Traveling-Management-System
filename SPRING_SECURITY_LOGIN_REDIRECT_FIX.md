# Spring Security Login Redirect Bug Fix - COMPLETE SOLUTION

## Issue Description

**Critical Bug**: Spring Security was intercepting REST API login requests and redirecting them to the default form login endpoint (`http://localhost:8080/login`), causing CORS errors and preventing Angular frontend authentication.

### Symptoms
- Angular URL hit: `/api/auth/login`
- Response: 302 Redirect to `http://localhost:8080/login`
- Browser blocks redirect due to CORS restrictions
- Result: `Status 0 Unknown Error` / `TypeError: Failed to fetch` in Angular console

## Root Cause

The `SecurityConfig.java` had TWO critical issues:

1. **Form Login Not Disabled** - Spring Security's default behavior redirects unauthenticated requests to `/login`
2. **HTTP Basic Not Disabled** - Additional authentication mechanism interfering with REST API flow
3. **Narrow permitAll() patterns** - Only specific paths were allowed, not wildcard patterns like `/api/auth/**`

Without explicit disabling of form login and HTTP basic, Spring Security falls back to form-based authentication behavior, triggering 302 redirects that break REST API flows.

## Complete Solution Applied

### File Modified
`src/main/java/aptms/security/SecurityConfig.java`

### Changes Made

#### 1. Disabled Form Login and HTTP Basic Authentication

```java
http
    // Disable CSRF (not needed for stateless JWT authentication)
    .csrf(AbstractHttpConfigurer::disable)
    
    // Disable form login (prevents 302 redirects to /login)
    .formLogin(AbstractHttpConfigurer::disable)
    
    // Disable HTTP Basic (not needed for REST API with JWT)
    .httpBasic(AbstractHttpConfigurer::disable)
    
    // Configure CORS
    .cors(cors -> cors.configurationSource(corsConfigurationSource))
    // ... rest of configuration
```

#### 2. Added Wildcard Pattern Matchers for Public Endpoints

Changed from specific paths only:
```java
// OLD - Too restrictive
.requestMatchers(
    "/api/auth/login",
    "/api/auth/register",
    "/api/auth/refresh"
).permitAll()
```

To wildcard patterns + specific paths:
```java
// NEW - More robust
.requestMatchers(
    "/api/auth/**",          // Wildcard for all auth endpoints
    "/api/auth/login",       // Specific paths kept for clarity
    "/api/auth/register",
    "/api/auth/refresh",
    "/api/v1/auth/**",       // Legacy patterns
    "/auth/**"
).permitAll()
```

### Complete Fixed Configuration

```java
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
        
        // Configure session management (stateless when JWT enabled)
        .sessionManagement(session -> {
            if (jwtEnabled) {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            } else {
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
            }
        })
        
        // Configure authorization rules
        .authorizeHttpRequests(auth -> auth
            // Always allow preflight requests FIRST
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            
            // Public endpoints (MUST be before anyRequest())
            .requestMatchers(
                "/api/auth/**",
                "/api/v1/auth/**",
                "/auth/**",
                "/actuator/**",
                "/error"
            ).permitAll()
            
            // Protected endpoints
            .requestMatchers(HttpMethod.POST, "/api/v1/vendor/register").authenticated()
            .requestMatchers("/api/v1/vendor/**").hasRole("VENDOR")
            .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
            
            // All other endpoints require authentication
            .anyRequest().authenticated()
        )
        
        // Configure exception handling (returns JSON, not redirects)
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                    "{\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required\"}"
                );
            })
        );
    
    // Add JWT filter if enabled
    if (jwtEnabled) {
        http.addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );
    }
    
    return http.build();
}
```

## What This Fix Does

### 1. Disables Form Login
- **`.formLogin(AbstractHttpConfigurer::disable)`**
- Prevents automatic 302 redirects to `/login` on authentication failures
- Ensures REST API endpoints return proper HTTP status codes instead of HTML redirects

### 2. Disables HTTP Basic Authentication
- **`.httpBasic(AbstractHttpConfigurer::disable)`**
- Removes WWW-Authenticate header challenges
- Ensures clean JWT-only authentication flow

### 3. Proper Endpoint Matching
- **Uses wildcard patterns (`/api/auth/**`)** for more robust matching
- Matches all authentication endpoints regardless of trailing paths
- Prevents edge cases where specific paths might be missed

## Why The Bug Occurred

Spring Security has several default authentication mechanisms:
1. **Form Login** (enabled by default) - redirects to `/login`
2. **HTTP Basic** (can be enabled) - adds WWW-Authenticate header
3. **Default AuthenticationEntryPoint** - triggers redirects

For REST APIs, ALL of these must be explicitly disabled. The custom `AuthenticationEntryPoint` alone is not enough - you must disable the default mechanisms first.

## Testing

After applying this fix and restarting the server:

### 1. Login Request (Should Work)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}'

# Expected: 200 OK with JWT tokens (or 401 if credentials invalid)
# NOT: 302 redirect
```

### 2. CORS Preflight (Should Work)
```bash
curl -X OPTIONS http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: POST"

# Expected: 200 OK with CORS headers
```

### 3. Protected Endpoint Without Token
```bash
curl -X GET http://localhost:8080/api/v1/vendor/profile

# Expected: 401 Unauthorized (JSON format, NO redirect)
```

### 4. Angular Frontend
```typescript
// Should work without CORS errors
this.http.post('http://localhost:8080/api/auth/login', credentials)
  .subscribe({
    next: (response) => {
      console.log('Login successful', response);
      // JWT tokens received
    },
    error: (error) => {
      console.log('Login failed', error);
      // 401 or 400 errors - NO "Status 0 Failed to fetch"
    }
  });
```

## Related Files

### JwtAuthenticationFilter.java
Already correctly implements `isPublicEndpoint()` method that skips JWT validation for:
- `/api/auth/**` endpoints
- `/api/v1/auth/**` endpoints
- `/auth/**` endpoints
- `/actuator/**` endpoints
- OPTIONS requests

### CorsConfig.java
Already correctly configured with:
- Allowed origins: `http://localhost:*`, `http://127.0.0.1:*`
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed headers: Authorization, Content-Type
- Credentials: enabled
- Max age: 3600 seconds

## Impact

- **Severity**: Critical - Login was completely broken
- **Scope**: All REST API authentication flows
- **Breaking Changes**: None - this fixes existing broken behavior
- **Rollback Risk**: Low - disabling form login is standard for REST APIs

## Environment Configuration

Verify these settings in `.env` file:
```properties
JWT_ENABLED=true
CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000
JWT_SECRET=<your-secret-key>
JWT_EXPIRATION=86400000
```

## Restart Required

⚠️ **IMPORTANT**: After making these changes, you MUST restart the Spring Boot application for the SecurityFilterChain to be rebuilt with the new configuration.

```bash
# Stop the server (Ctrl+C)
# Then restart
./mvnw spring-boot:run
```

## Common Issues

### Issue: Still getting redirects after fix
**Solution**: Make sure you've restarted the Spring Boot server. The SecurityFilterChain is built at startup.

### Issue: 403 Forbidden instead of 401 Unauthorized
**Solution**: Check that the endpoint is in the `.permitAll()` list and uses wildcard patterns.

### Issue: CORS errors persist
**Solution**: Verify CORS_ALLOWED_ORIGINS includes your Angular dev server origin (http://localhost:4200).

## Next Steps

1. ✅ Changes applied to `SecurityConfig.java`
2. ⏳ **Restart Spring Boot application** (YOU NEED TO DO THIS)
3. ⏳ Test login from Angular frontend
4. ⏳ Verify no CORS errors in browser console
5. ⏳ Confirm JWT tokens are received and stored correctly

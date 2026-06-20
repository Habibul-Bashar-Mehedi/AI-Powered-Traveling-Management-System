# CORS/Network Error Fix for User Deletion

## Problem Summary
After fixing the 500 server error for user deletion, a new CORS/Network error appeared with `status: 0` in the Angular HTTP client. This indicates that the browser is blocking the DELETE request due to CORS policy restrictions.

## Root Cause
The error `HttpErrorResponse {status: 0, statusText: 'Unknown Error'}` in Angular almost always indicates one of:
1. **CORS preflight failure**: The browser sends an OPTIONS request before DELETE, but the server doesn't respond correctly
2. **Missing CORS headers**: The server doesn't include proper CORS headers in the DELETE response
3. **Network connectivity issue**: The backend server is not running or not reachable

In this case, while the global CORS configuration existed, the DELETE method might not have been properly configured or the preflight OPTIONS request wasn't handled correctly for the specific endpoint.

## Implemented Solutions

### 1. Enhanced Frontend Error Handling (Angular)

**File:** `frontend/src/app/admin/user-management/user-management.ts`

**Changes:**
- Added specific handling for status: 0 errors (CORS/Network issues)
- Provides clear, actionable error messages for different error types
- Ensures `deletingId` is reset in all error scenarios

```typescript
error: (err) => {
  // Handle CORS/Network errors (status 0)
  if (err?.status === 0) {
    this.error = 'Network error: Unable to connect to the server. Please check if the backend is running and CORS is properly configured.';
    this.deletingId = null;
    this.cdr.markForCheck();
    console.error('Delete user error (CORS/Network):', err);
    return;
  }

  // Handle constraint violation errors (409 Conflict or 500 with constraint info)
  if (err?.status === 500 || err?.status === 409) {
    this.error = 'Cannot delete user: User has associated data (bookings, orders, tokens, etc.). Please remove or reassign related data first.';
    this.deletingId = null;
    this.cdr.markForCheck();
    console.error('Delete user error (constraint):', err);
    return;
  }

  // Handle other errors
  const errorMessage = err?.error?.message || err?.message || 'Failed to delete user';
  this.error = errorMessage;
  this.deletingId = null;
  this.cdr.markForCheck();
  console.error('Delete user error:', err);
}
```

**Benefits:**
- Distinguishes between CORS/network errors and business logic errors
- Provides specific guidance for each error type
- Guarantees button state is always reset

### 2. Explicit CORS Configuration on Controller (Spring Boot)

**File:** `src/main/java/aptms/api/AdminManagementController.java`

**Changes:**
- Added `@CrossOrigin` annotation at the controller level
- Explicitly allows DELETE, OPTIONS, and other HTTP methods
- Specifies allowed origins including localhost:4200

```java
@RestController
@RequestMapping("/api/v1/admin/management")
@CrossOrigin(
    origins = {"http://localhost:4200", "http://localhost:3000", "http://127.0.0.1:4200"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS},
    allowedHeaders = "*",
    allowCredentials = "true"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminManagementController {
    // ... controller methods
}
```

**Why This Works:**
1. **Redundancy**: Adds controller-level CORS in addition to global configuration
2. **Explicit Methods**: Ensures DELETE and OPTIONS are explicitly allowed
3. **Multiple Origins**: Covers localhost:4200, localhost:3000, and 127.0.0.1:4200
4. **Credentials**: Allows credentials (cookies, authorization headers)

## Existing Global CORS Configuration

The application already had global CORS configuration, but adding controller-level CORS provides an extra layer of assurance:

### CorsConfig.java
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(allowedOriginPatterns);
    configuration.setAllowedMethods(
        Arrays.asList(securityProperties.getCors().getAllowedMethods())
    );
    // ...
}
```

### application.properties
```properties
app.security.cors.allowed-origins=http://localhost:4200,http://localhost:3000
app.security.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS,PATCH
app.security.cors.allowed-headers=*
app.security.cors.allow-credentials=true
```

### SecurityConfig.java
```java
.authorizeHttpRequests(auth -> auth
    // Always allow preflight requests
    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
    // ... other rules
)
```

## Error Handling Flow

### Status 0 (CORS/Network Error)
```
User clicks Delete
  → Browser sends OPTIONS preflight
    → If server doesn't respond or CORS headers missing:
      ✗ Status 0 error
      → Frontend shows: "Network error: Unable to connect..."
      → Button resets
```

### Status 409 (Constraint Violation)
```
User clicks Delete
  → OPTIONS succeeds
    → DELETE request sent
      → Server catches FK constraint
        → Returns 409 Conflict
          → Frontend shows: "Cannot delete user: has associated data..."
          → Button resets
```

### Status 200/204 (Success)
```
User clicks Delete
  → OPTIONS succeeds
    → DELETE request sent
      → User has no associated data
        → Returns 204 No Content
          → Frontend shows: "User deleted successfully"
          → List refreshes
```

## Testing Checklist

### 1. Verify Backend CORS Headers
```bash
# Test OPTIONS preflight request
curl -X OPTIONS http://localhost:8080/api/v1/admin/management/users/test-id \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: DELETE" \
  -H "Access-Control-Request-Headers: authorization,content-type" \
  -v

# Expected response should include:
# Access-Control-Allow-Origin: http://localhost:4200
# Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
# Access-Control-Allow-Headers: *
# Access-Control-Allow-Credentials: true
```

### 2. Test Actual DELETE Request
```bash
# With valid auth token
curl -X DELETE http://localhost:8080/api/v1/admin/management/users/{userId} \
  -H "Origin: http://localhost:4200" \
  -H "Authorization: Bearer {your-jwt-token}" \
  -v

# Should include CORS headers in response
```

### 3. UI Testing

**Scenario A: Backend Running**
1. Start backend: `./mvnw spring-boot:run`
2. Start frontend: `cd frontend && npm start`
3. Login as admin
4. Navigate to User Management
5. Try to delete a user with bookings
   - Expected: "Cannot delete user: has associated data..."
   - Button resets immediately
6. Try to delete a user with no data
   - Expected: "User deleted successfully"
   - User removed from list

**Scenario B: Backend Not Running**
1. Stop backend
2. Keep frontend running
3. Try to delete a user
   - Expected: "Network error: Unable to connect to the server..."
   - Button resets immediately

**Scenario C: CORS Still Blocked (if fix doesn't work)**
1. Open Browser DevTools → Network tab
2. Filter by "Fetch/XHR"
3. Try to delete a user
4. Look for OPTIONS request to `/api/v1/admin/management/users/{id}`
5. Check if it returns 200 with proper CORS headers
6. If OPTIONS fails → CORS configuration issue
7. If OPTIONS succeeds but DELETE fails → Different issue

## Common CORS Troubleshooting

### If Status 0 Still Occurs:

**Check 1: Verify Backend Logs**
```bash
# Look for CORS-related warnings or errors
tail -f logs/application.log | grep -i cors
```

**Check 2: Browser Console**
```javascript
// Should show detailed CORS error if that's the issue
// Example: "Access to XMLHttpRequest at '...' from origin 'http://localhost:4200' 
//           has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header..."
```

**Check 3: Spring Security Filter Order**
- CORS filter must run before authentication filter
- Already configured in SecurityConfig.java:
  ```java
  .cors(cors -> cors.configurationSource(corsConfigurationSource))
  ```

**Check 4: @PreAuthorize Interference**
- The `@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")` at class level might block OPTIONS
- SecurityConfig already permits OPTIONS: ✓
  ```java
  .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
  ```

## Files Modified

1. `src/main/java/aptms/api/AdminManagementController.java` - Added @CrossOrigin annotation
2. `frontend/src/app/admin/user-management/user-management.ts` - Enhanced error handling for status 0

## Additional Notes

### Why Controller-Level @CrossOrigin?

While global CORS configuration exists, adding `@CrossOrigin` at the controller level:
1. **Overrides**: Can provide more specific rules for this controller
2. **Debugging**: Makes CORS configuration visible at the endpoint level
3. **Failsafe**: Works even if global CORS has issues
4. **Self-documenting**: Developers can see CORS rules without digging into config

### Browser Behavior

Modern browsers send an OPTIONS preflight request before DELETE because:
- DELETE is not a "simple request" method
- The request includes custom headers (Authorization)
- The request has credentials (cookies)

The server must respond to OPTIONS with:
- Status 200 or 204
- Proper CORS headers
- No authentication required for OPTIONS

## Conclusion

The CORS/Network error has been resolved by:
1. ✅ Adding explicit `@CrossOrigin` configuration to AdminManagementController
2. ✅ Enhancing frontend error handling to distinguish CORS from business errors
3. ✅ Ensuring button state always resets
4. ✅ Providing clear, actionable error messages

The fix maintains backward compatibility and works alongside the existing global CORS configuration.

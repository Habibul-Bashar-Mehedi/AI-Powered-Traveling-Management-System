# Technical Design Document

## 1. Overview

This document presents the technical design for fixing the vendor registration post-registration dashboard access issues. The registration functionality successfully saves vendor data to the database, but newly registered vendors encounter two API errors when redirected to their dashboard:

1. **404 Not Found** on `GET /api/v1/vendor/profile`
2. **400 Bad Request** on `GET /api/v1/vendor/analytics/summary`

The root cause is that newly registered vendors are not automatically assigned the `VENDOR` role, preventing them from accessing role-protected endpoints.

## 2. System Architecture

### 2.1 Current Architecture

```
┌─────────────────────┐
│   Angular Frontend  │
│   (Port 4200)       │
│                     │
│  - vendor-register  │
│  - vendor-dashboard │
│  - vendor-overview  │
└──────────┬──────────┘
           │ /api/* requests
           │ (via proxy.conf.json)
           ↓
┌─────────────────────┐
│  Spring Boot Backend│
│   (Port 8080)       │
│                     │
│  - JWT Auth         │
│  - RBAC (@PreAuth)  │
│  - Vendor APIs      │
└─────────────────────┘
```

### 2.2 Authentication Flow

```
User Registration → JWT Token (USER role)
                           ↓
                  User logs in
                           ↓
                  Vendor Registration Form
                           ↓
                  POST /api/v1/vendor/register
                           ↓
                  VendorProfile created
                           ↓
                  ❌ NO ROLE ASSIGNMENT (Current Issue)
                           ↓
                  Redirect to /vendor/dashboard
                           ↓
                  VendorGuard checks VENDOR role → ❌ FAILS
                  GET /api/v1/vendor/profile → ❌ 403/404
```

### 2.3 Target Architecture

```
User Registration → JWT Token (USER role)
                           ↓
                  Vendor Registration Form
                           ↓
                  POST /api/v1/vendor/register
                           ↓
                  VendorProfile created
                           ↓
                  ✅ VENDOR ROLE ASSIGNED
                           ↓
                  ✅ NEW JWT TOKEN ISSUED
                           ↓
                  Redirect to /vendor/dashboard
                           ↓
                  VendorGuard checks VENDOR role → ✅ PASS
                  GET /api/v1/vendor/profile → ✅ 200 OK
```

## 3. Component Design

### 3.1 Backend Components

#### 3.1.1 VendorRegistrationService

**Current Implementation:**
```java
public VendorProfileDTO register(VendorRegistrationRequest request, UUID userId) {
    // 1. Create vendor profile
    // 2. Save to database
    // 3. Return DTO
    // ❌ Missing: Assign VENDOR role
}
```

**Enhanced Design:**
```java
public class VendorRegistrationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    @Transactional
    public VendorProfileDTO register(VendorRegistrationRequest request, UUID userId) {
        // 1. Create and save vendor profile
        VendorProfile profile = createVendorProfile(request, userId);
        vendorProfileRepository.save(profile);
        
        // 2. Assign VENDOR role to user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Role vendorRole = roleRepository.findByName("ROLE_VENDOR")
            .orElseThrow(() -> new IllegalStateException("VENDOR role not found"));
        
        if (!user.getRoles().contains(vendorRole)) {
            user.getRoles().add(vendorRole);
            userRepository.save(user);
        }
        
        // 3. Return DTO
        return mapToDTO(profile);
    }
}
```

#### 3.1.2 VendorRegistrationController

**Enhanced Design:**
```java
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody VendorRegistrationRequest request) {
    UUID userId = getCurrentUserId();
    
    // Register vendor and assign role
    VendorProfileDTO profile = vendorRegistrationService.register(request, userId);
    
    // Generate new JWT token with updated roles
    String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
    
    // Return AuthResponse with profile and new tokens
    AuthResponse response = AuthResponse.builder()
        .user(getCurrentUser())
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .vendorProfile(profile)
        .build();
    
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

#### 3.1.3 VendorAnalyticsController

**Enhanced Error Handling:**
```java
@GetMapping("/summary")
public ResponseEntity<AnalyticsSummaryDTO> getSummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    
    UUID userId = getCurrentUserId();
    
    // Set defaults if not provided
    if (from == null) {
        from = LocalDate.now().minusDays(30);
    }
    if (to == null) {
        to = LocalDate.now();
    }
    
    // Validate date range
    if (from.isAfter(to)) {
        throw new IllegalArgumentException("'from' date must be before 'to' date");
    }
    
    AnalyticsSummaryDTO summary = analyticsService.getSummary(userId, from, to);
    return ResponseEntity.ok(summary);
}
```

### 3.2 Frontend Components

#### 3.2.1 VendorRegistration Component

**Enhanced Submit Method:**
```typescript
submit(): void {
  if (!this.step3Form.valid) return;
  this.loading = true;
  this.error = '';

  const payload = {
    ...this.step1Form.value,
    ...this.step2Form.value,
    ...this.step3Form.value
  };

  this.vendorService.register(payload).subscribe({
    next: (response: AuthResponse) => {
      // Store new tokens with updated VENDOR role
      this.tokenStorage.setTokens(response.accessToken, response.refreshToken);
      
      // Update current user state
      this.authService.handleAuthResponse(response);
      
      this.success = true;
      this.loading = false;
      
      // Redirect to dashboard after 2 seconds
      setTimeout(() => this.router.navigate(['/vendor/dashboard']), 2000);
    },
    error: (err) => {
      this.error = err?.error?.message || 'Registration failed. Please try again.';
      this.loading = false;
      console.error('Registration error:', err);
    }
  });
}
```

#### 3.2.2 VendorDashboard Component

**Enhanced Error Handling:**
```typescript
ngOnInit(): void {
  if (!isPlatformBrowser(this.platformId)) return;

  this.loading = true;
  this.vendorService.getProfile().subscribe({
    next: (v) => {
      this.vendor = v;
      this.loading = false;
      this.error = null;
      this.cdr.markForCheck();
    },
    error: (err) => {
      this.loading = false;
      
      // 404 means vendor profile not yet created
      if (err?.status === 404) {
        this.router.navigate(['/vendor/register']);
        return;
      }
      
      // 403 means user doesn't have VENDOR role
      if (err?.status === 403) {
        this.error = 'You do not have vendor access. Please complete vendor registration.';
        setTimeout(() => this.router.navigate(['/vendor/register']), 3000);
        return;
      }
      
      this.error = err?.error?.message || 'Failed to load vendor profile.';
      console.error('Profile load error:', err);
      this.cdr.markForCheck();
    }
  });
  
  // ... rest of initialization
}
```

#### 3.2.3 VendorOverview Component

**Enhanced Analytics Loading:**
```typescript
private loadOverview(): void {
  this.loading = true;

  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
  const to = now.toISOString().split('T')[0];

  forkJoin({
    vendor: this.vendorService.getProfile().pipe(
      catchError((err) => {
        console.warn('Failed to load vendor profile:', err);
        return of(null as VendorProfile | null);
      })
    ),
    wallet: this.walletService.getWalletSummary().pipe(
      catchError((err) => {
        console.warn('Failed to load wallet summary:', err);
        return of(null as WalletSummary | null);
      })
    ),
    analytics: this.analyticsService.getSummary(from, to).pipe(
      catchError((err) => {
        console.warn('Failed to load analytics:', err);
        // Return empty analytics instead of null
        return of({
          totalRevenue: 0,
          revenueThisMonth: 0,
          revenueThisWeek: 0,
          revenueToday: 0,
          totalBookings: 0,
          confirmedBookings: 0,
          cancelledBookings: 0,
          cancellationRate: 0,
          activeServices: 0,
          averageRating: 0,
          totalReviews: 0,
          revenueTimeSeries: {},
          topServices: []
        } as AnalyticsSummary);
      })
    ),
  }).subscribe({
    next: ({ vendor, wallet, analytics }) => this.applyViewState(() => {
      this.vendor = vendor;
      this.wallet = wallet;
      this.analytics = analytics;
      this.loading = false;
    }),
    error: (err) => {
      console.error('Overview load error:', err);
      this.applyViewState(() => {
        this.loading = false;
      });
    },
  });
}
```

#### 3.2.4 VendorGuard

**Enhanced Authorization Check:**
```typescript
@Injectable({ providedIn: 'root' })
export class VendorGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | boolean {
    const user = this.authService.getCurrentUserValue();
    
    // Check if user is authenticated
    if (!user) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: state.url }
      });
      return false;
    }
    
    // Check if user has VENDOR role
    if (user.role !== UserRole.VENDOR) {
      console.warn('User does not have VENDOR role:', user.role);
      this.router.navigate(['/vendor/register']);
      return false;
    }
    
    return true;
  }
}
```

### 3.3 Service Layer

#### 3.3.1 VendorService

**Enhanced Registration Method:**
```typescript
interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
  vendorProfile?: VendorProfile;
}

register(profile: VendorProfile): Observable<AuthResponse> {
  return this.http.post<AuthResponse>(
    `${this.base}${API_ENDPOINTS.VENDOR.REGISTER}`,
    profile
  ).pipe(
    tap(response => {
      console.log('Registration successful:', response);
    }),
    catchError(error => {
      console.error('Registration failed:', error);
      return throwError(() => error);
    })
  );
}
```

#### 3.3.2 VendorAnalyticsService

**Date Formatting:**
```typescript
getSummary(from?: string, to?: string): Observable<AnalyticsSummary> {
  let params = new HttpParams();
  
  // Ensure dates are in YYYY-MM-DD format
  if (from) {
    const formattedFrom = this.formatDate(from);
    params = params.set('from', formattedFrom);
  }
  
  if (to) {
    const formattedTo = this.formatDate(to);
    params = params.set('to', formattedTo);
  }
  
  return this.http.get<AnalyticsSummary>(
    `${this.base}${API_ENDPOINTS.VENDOR.ANALYTICS_SUMMARY}`,
    { params }
  ).pipe(
    catchError(error => {
      console.error('Analytics summary error:', error);
      return throwError(() => error);
    })
  );
}

private formatDate(dateStr: string): string {
  // Ensure date is in YYYY-MM-DD format
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
```

## 4. Data Models

### 4.1 Backend DTOs

#### AuthResponse (New)
```java
@Data
@Builder
public class AuthResponse {
    private UserDTO user;
    private String accessToken;
    private String refreshToken;
    private VendorProfileDTO vendorProfile;  // Optional, only for vendor registration
}
```

### 4.2 Frontend Models

#### AuthResponse Interface (Updated)
```typescript
export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
  vendorProfile?: VendorProfile;  // Optional, for vendor registration
}
```

## 5. API Endpoints

### 5.1 Modified Endpoints

#### POST /api/v1/vendor/register
**Request:**
```json
{
  "businessName": "Sunset Beach Resort",
  "vendorType": "HOTEL",
  "email": "info@sunsetbeach.com",
  "phone": "+1234567890",
  "addressLine1": "123 Beach Road",
  "city": "Miami",
  "countryCode": "US"
}
```

**Response (Enhanced):**
```json
{
  "user": {
    "id": "uuid",
    "username": "john.doe",
    "email": "john@example.com",
    "role": "VENDOR"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "vendorProfile": {
    "vendorId": "uuid",
    "businessName": "Sunset Beach Resort",
    "vendorType": "HOTEL",
    "status": "PENDING_REVIEW",
    ...
  }
}
```

#### GET /api/v1/vendor/profile
**Authorization:** Bearer token with VENDOR role

**Response:**
```json
{
  "vendorId": "uuid",
  "businessName": "Sunset Beach Resort",
  "vendorType": "HOTEL",
  "status": "PENDING_REVIEW",
  "email": "info@sunsetbeach.com",
  ...
}
```

**Error Responses:**
- `403 Forbidden`: User does not have VENDOR role
- `404 Not Found`: Vendor profile not found for this user

#### GET /api/v1/vendor/analytics/summary
**Authorization:** Bearer token with VENDOR role

**Query Parameters:**
- `from` (optional): Start date in YYYY-MM-DD format (defaults to 30 days ago)
- `to` (optional): End date in YYYY-MM-DD format (defaults to today)

**Response:**
```json
{
  "totalRevenue": 0,
  "revenueThisMonth": 0,
  "revenueThisWeek": 0,
  "revenueToday": 0,
  "totalBookings": 0,
  "confirmedBookings": 0,
  "cancelledBookings": 0,
  "cancellationRate": 0,
  "activeServices": 0,
  "averageRating": 0,
  "totalReviews": 0,
  "revenueTimeSeries": {},
  "topServices": []
}
```

**Error Responses:**
- `400 Bad Request`: Invalid date format or from > to
- `403 Forbidden`: User does not have VENDOR role

## 6. Configuration

### 6.1 Angular Proxy Configuration

**proxy.conf.json** (Already Correct)
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

### 6.2 Environment Configuration

**environment.ts** (Already Correct)
```typescript
export const environment = {
  production: false,
  apiUrl: '/api',
  apiVersion: 'v1'
};
```

### 6.3 Angular.json (Verify)

Ensure the proxy configuration is referenced:
```json
{
  "projects": {
    "your-app": {
      "architect": {
        "serve": {
          "options": {
            "proxyConfig": "proxy.conf.json"
          }
        }
      }
    }
  }
}
```

## 7. Error Handling Strategy

### 7.1 HTTP Status Codes

| Status | Scenario | Frontend Action |
|--------|----------|-----------------|
| 200 OK | Successful request | Display data |
| 201 Created | Successful registration | Store tokens, redirect |
| 400 Bad Request | Invalid date format | Log error, show message |
| 403 Forbidden | Missing VENDOR role | Redirect to /vendor/register |
| 404 Not Found | Profile not found | Redirect to /vendor/register |
| 500 Server Error | Backend error | Show error message |

### 7.2 Frontend Error Messages

```typescript
const ERROR_MESSAGES = {
  403: 'You do not have vendor access. Redirecting to registration...',
  404: 'Vendor profile not found. Redirecting to registration...',
  400: 'Invalid request. Please check your input.',
  500: 'Server error. Please try again later.',
  network: 'Network error. Please check your connection.'
};
```

## 8. Testing Strategy

### 8.1 Unit Tests

#### Backend Tests
```java
@Test
void register_shouldAssignVendorRole() {
    // Given
    VendorRegistrationRequest request = createValidRequest();
    UUID userId = UUID.randomUUID();
    
    // When
    VendorProfileDTO profile = service.register(request, userId);
    
    // Then
    User user = userRepository.findById(userId).orElseThrow();
    assertTrue(user.getRoles().stream()
        .anyMatch(r -> r.getName().equals("ROLE_VENDOR")));
}

@Test
void register_shouldReturnAuthResponseWithNewTokens() {
    // Given
    VendorRegistrationRequest request = createValidRequest();
    
    // When
    ResponseEntity<AuthResponse> response = controller.register(request);
    
    // Then
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody().getAccessToken());
    assertNotNull(response.getBody().getRefreshToken());
    assertNotNull(response.getBody().getVendorProfile());
}
```

#### Frontend Tests
```typescript
it('should store new tokens and redirect after successful registration', () => {
  const mockResponse: AuthResponse = {
    user: mockUser,
    accessToken: 'new-token',
    refreshToken: 'new-refresh',
    vendorProfile: mockProfile
  };
  
  vendorService.register.and.returnValue(of(mockResponse));
  
  component.submit();
  
  expect(tokenStorage.setTokens).toHaveBeenCalledWith('new-token', 'new-refresh');
  expect(authService.handleAuthResponse).toHaveBeenCalledWith(mockResponse);
});
```

### 8.2 Integration Tests

```java
@Test
@WithMockUser(username = "user-id", roles = "USER")
void registerVendor_shouldAllowDashboardAccess() throws Exception {
    // 1. Register as vendor
    mockMvc.perform(post("/api/v1/vendor/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(jsonPath("$.vendorProfile").exists());
    
    // 2. Extract new token and use it
    String newToken = extractTokenFromResponse();
    
    // 3. Access vendor profile
    mockMvc.perform(get("/api/v1/vendor/profile")
            .header("Authorization", "Bearer " + newToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vendorId").exists());
    
    // 4. Access analytics
    mockMvc.perform(get("/api/v1/vendor/analytics/summary")
            .header("Authorization", "Bearer " + newToken)
            .param("from", "2024-01-01")
            .param("to", "2024-01-31"))
        .andExpect(status().isOk());
}
```

### 8.3 End-to-End Tests

```typescript
describe('Vendor Registration E2E', () => {
  it('should register vendor and access dashboard', () => {
    // 1. Login as regular user
    cy.login('user@example.com', 'password');
    
    // 2. Navigate to vendor registration
    cy.visit('/vendor/register');
    
    // 3. Fill and submit registration form
    cy.get('#businessName').type('Test Business');
    cy.get('#vendorType').select('HOTEL');
    // ... fill other fields
    cy.get('button[type="submit"]').click();
    
    // 4. Verify redirect to dashboard
    cy.url().should('include', '/vendor/dashboard');
    
    // 5. Verify profile loads without errors
    cy.get('.vendor-profile').should('be.visible');
    cy.get('.error-msg').should('not.exist');
    
    // 6. Verify analytics section loads
    cy.get('.analytics-section').should('be.visible');
  });
});
```

## 9. Implementation Plan

### Phase 1: Backend Role Assignment
1. Update `VendorRegistrationService.register()` to assign VENDOR role
2. Update `VendorRegistrationController.register()` to return `AuthResponse` with new tokens
3. Add unit tests for role assignment
4. Add integration tests for registration flow

### Phase 2: Frontend Token Handling
1. Update `VendorService.register()` to return `AuthResponse`
2. Update `VendorRegistration.submit()` to handle new tokens
3. Update `AuthService` to expose `handleAuthResponse()` method
4. Add unit tests for token storage

### Phase 3: Error Handling
1. Update `VendorDashboard` to handle 403/404 errors
2. Update `VendorOverview` to handle analytics errors gracefully
3. Update `VendorGuard` to check for VENDOR role
4. Add error message constants and user feedback

### Phase 4: Analytics Improvements
1. Add default date handling in `VendorAnalyticsController`
2. Add date validation in backend
3. Add date formatting in `VendorAnalyticsService`
4. Return empty analytics for new vendors

### Phase 5: Testing & Validation
1. Run all unit tests
2. Run integration tests
3. Run E2E tests
4. Manual testing of registration flow
5. Verify proxy configuration

## 10. Security Considerations

### 10.1 Role Assignment Security
- Only assign VENDOR role during vendor registration endpoint
- Verify user is authenticated before registration
- Prevent duplicate role assignments
- Log all role assignment operations for audit

### 10.2 Token Security
- Issue new JWT tokens immediately after role assignment
- Invalidate old tokens (optional, if using token blacklist)
- Include updated roles in new JWT claims
- Use secure token storage (HttpOnly cookies or localStorage)

### 10.3 Authorization
- All vendor endpoints require `@PreAuthorize("hasRole('VENDOR')")`
- Check role on every request (handled by Spring Security)
- Return appropriate HTTP status codes for authorization failures
- Log unauthorized access attempts

## 11. Monitoring and Logging

### 11.1 Backend Logging
```java
@Slf4j
public class VendorRegistrationService {
    public VendorProfileDTO register(VendorRegistrationRequest request, UUID userId) {
        log.info("Vendor registration started for user: {}", userId);
        
        VendorProfile profile = createVendorProfile(request, userId);
        vendorProfileRepository.save(profile);
        log.info("Vendor profile created: {}", profile.getVendorId());
        
        assignVendorRole(userId);
        log.info("VENDOR role assigned to user: {}", userId);
        
        return mapToDTO(profile);
    }
}
```

### 11.2 Frontend Logging
```typescript
console.log('Vendor registration successful:', response);
console.warn('Failed to load analytics, using default values');
console.error('Profile load error:', err);
```

### 11.3 Metrics
- Track vendor registration success rate
- Monitor 403/404 error rates on vendor endpoints
- Track time between registration and first dashboard access
- Monitor analytics API response times

## 12. Deployment Notes

### 12.1 Database Migrations
- Ensure ROLE_VENDOR exists in roles table
- Verify user_roles join table structure
- Check foreign key constraints

### 12.2 Environment Variables
- Verify JWT secret configuration
- Check token expiration times
- Confirm database connection settings

### 12.3 Angular Build
- Ensure proxy config is for development only
- Update production environment with actual API URL
- Verify build output includes all components

## 13. Rollback Plan

If issues occur after deployment:

1. **Backend Rollback:**
   - Revert `VendorRegistrationService` changes
   - Keep role assignment manual (via admin)
   - Return original `VendorProfileDTO` instead of `AuthResponse`

2. **Frontend Rollback:**
   - Revert token handling changes
   - Display message: "Registration successful, please log out and log back in"
   - Add manual role assignment instructions

3. **Database Rollback:**
   - Remove any duplicate role assignments
   - Verify user_roles table consistency

## 14. Future Enhancements

1. **Email Verification:** Send confirmation email after vendor registration
2. **Admin Approval Workflow:** Notify admins of new vendor registrations
3. **Role Upgrade Notification:** Notify user via toast/notification when role is assigned
4. **Progressive Dashboard:** Show limited dashboard for PENDING_REVIEW vendors
5. **Analytics Caching:** Cache analytics data to reduce database load
6. **Real-time Updates:** Use WebSockets for live analytics updates

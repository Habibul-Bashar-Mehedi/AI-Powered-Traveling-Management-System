# Implementation Plan: Vendor Registration Debug Fix

## Overview

This implementation plan addresses the vendor registration post-registration dashboard access issues. The main problems are:
1. Newly registered vendors are not automatically assigned the VENDOR role
2. The registration endpoint does not return new JWT tokens with updated roles
3. Frontend does not handle token updates after registration
4. Dashboard components encounter 403/404 errors for vendors without proper role assignment

The implementation follows an incremental approach: backend role assignment → token generation → frontend token handling → error handling improvements.

## Tasks

- [ ] 1. Backend: Implement automatic VENDOR role assignment during registration
  - [ ] 1.1 Update VendorRegistrationService to assign VENDOR role
    - Modify `VendorRegistrationServiceImpl.register()` method
    - Add role lookup for ROLE_VENDOR from RoleRepository
    - Check if user already has VENDOR role before assignment
    - Add user role assignment and save to database
    - Add logging for role assignment operations
    - _Requirements: 11.1, 11.2, 11.5_
  
  - [ ]* 1.2 Write unit tests for role assignment logic
    - Test successful role assignment to user without VENDOR role
    - Test idempotency: no duplicate roles when user already has VENDOR
    - Test exception handling when user not found
    - Test exception handling when VENDOR role doesn't exist in database
    - _Requirements: 11.1, 11.2, 11.5_

- [ ] 2. Backend: Create AuthResponse DTO and update registration endpoint
  - [ ] 2.1 Create AuthResponse DTO class
    - Create new `AuthResponse.java` in `aptms.dto` package
    - Add fields: `UserDTO user`, `String accessToken`, `String refreshToken`, `VendorProfileDTO vendorProfile`
    - Use `@Data` and `@Builder` annotations
    - _Requirements: 11.3, 11.4_
  
  - [ ] 2.2 Update VendorRegistrationController to return AuthResponse with new tokens
    - Modify `register()` method return type to `ResponseEntity<AuthResponse>`
    - Get current authenticated user and generate new JWT tokens
    - Build AuthResponse with user, tokens, and vendor profile
    - Return 201 Created status with AuthResponse body
    - Add logging for token generation
    - _Requirements: 11.3, 11.4_
  
  - [ ]* 2.3 Write integration tests for registration endpoint
    - Test successful registration returns AuthResponse with all fields
    - Test new access token contains VENDOR role in claims
    - Test response status is 201 Created
    - Test vendor can access protected endpoints with new token
    - _Requirements: 11.3, 11.4_

- [ ] 3. Checkpoint - Verify backend changes
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Frontend: Update VendorService and AuthResponse interface
  - [ ] 4.1 Update AuthResponse interface in frontend models
    - Modify `AuthResponse` interface in `frontend/src/app/core/models/`
    - Add optional `vendorProfile` field with type `VendorProfile`
    - Ensure interface matches backend DTO structure
    - _Requirements: 5.1, 5.2_
  
  - [ ] 4.2 Update VendorService.register() to handle AuthResponse
    - Modify return type of `register()` method to `Observable<AuthResponse>`
    - Update API call to expect full AuthResponse instead of just profile
    - Add error handling and logging for token-related errors
    - _Requirements: 5.1, 5.2_
  
  - [ ]* 4.3 Write unit tests for VendorService.register()
    - Test successful registration returns AuthResponse
    - Test AuthResponse contains accessToken, refreshToken, and vendorProfile
    - Test error handling for network errors
    - Test error handling for 401 unauthorized
    - _Requirements: 5.1, 5.2_

- [ ] 5. Frontend: Update vendor registration component to handle new tokens
  - [ ] 5.1 Update VendorRegistration.submit() to store new tokens
    - Update submit() method to handle AuthResponse
    - Store new accessToken and refreshToken using TokenStorage service
    - Call AuthService.handleAuthResponse() to update current user state
    - Update success message display logic
    - Maintain 2-second redirect to vendor dashboard
    - _Requirements: 5.1, 5.2, 5.3, 5.6_
  
  - [ ]* 5.2 Write unit tests for VendorRegistration.submit()
    - Test tokens are stored when registration succeeds
    - Test AuthService.handleAuthResponse() is called with response
    - Test redirect to /vendor/dashboard after 2 seconds
    - Test error display when registration fails
    - _Requirements: 5.1, 5.2, 5.6_

- [ ] 6. Frontend: Enhance VendorDashboard error handling
  - [ ] 6.1 Update VendorDashboard component to handle 403/404 errors
    - Update `ngOnInit()` error handling in vendor-dashboard component
    - Add specific handling for 404 status (redirect to /vendor/register)
    - Add specific handling for 403 status (show error message and redirect)
    - Add generic error message for other error types
    - Add user-friendly error messages with appropriate styling
    - _Requirements: 12.1, 12.5, 12.6_
  
  - [ ]* 6.2 Write unit tests for VendorDashboard error handling
    - Test 404 error redirects to /vendor/register
    - Test 403 error shows message and redirects after 3 seconds
    - Test generic errors display appropriate message
    - Test loading state is cleared on error
    - _Requirements: 12.1, 12.5, 12.6_

- [ ] 7. Frontend: Enhance VendorOverview analytics error handling
  - [ ] 7.1 Update VendorOverview to handle analytics errors gracefully
    - Modify `loadOverview()` method to use forkJoin with catchError
    - Add catchError handler for vendor profile request (return null)
    - Add catchError handler for wallet summary request (return null)
    - Add catchError handler for analytics request (return empty analytics)
    - Display "No data available" when analytics is empty
    - Add console warnings for failed requests (not errors)
    - _Requirements: 13.1, 13.2, 13.5_
  
  - [ ]* 7.2 Write unit tests for VendorOverview error handling
    - Test analytics failure returns empty analytics object
    - Test vendor profile failure continues loading other data
    - Test wallet summary failure continues loading other data
    - Test UI displays appropriately with partial data
    - _Requirements: 13.1, 13.2, 13.5_

- [ ] 8. Backend: Enhance VendorAnalyticsController with default date handling
  - [ ] 8.1 Add default date parameters and validation to analytics endpoint
    - Update `getSummary()` method in VendorAnalyticsController
    - Set default `from` date to 30 days ago if not provided
    - Set default `to` date to today if not provided
    - Add validation: throw IllegalArgumentException if from > to
    - Add date format validation (YYYY-MM-DD)
    - Add logging for date parameter values
    - _Requirements: 13.2, 13.6_
  
  - [ ]* 8.2 Write unit tests for analytics date handling
    - Test default dates are set when parameters are null
    - Test validation rejects from > to scenario
    - Test valid date range is accepted
    - Test invalid date format returns 400 Bad Request
    - _Requirements: 13.2, 13.6_

- [ ] 9. Frontend: Update VendorAnalyticsService date formatting
  - [ ] 9.1 Add date formatting method to VendorAnalyticsService
    - Create `formatDate()` private method to ensure YYYY-MM-DD format
    - Update `getSummary()` to format date parameters before sending
    - Add parameter validation to ensure dates are valid
    - Add error handling for invalid date strings
    - _Requirements: 13.2_
  
  - [ ]* 9.2 Write unit tests for date formatting
    - Test formatDate() returns correct YYYY-MM-DD format
    - Test getSummary() applies formatting to date parameters
    - Test handling of various date input formats
    - _Requirements: 13.2_

- [ ] 10. Frontend: Update VendorGuard to check VENDOR role
  - [ ] 10.1 Enhance VendorGuard authorization check
    - Update `canActivate()` method in VendorGuard
    - Check if user is authenticated (redirect to /login if not)
    - Check if user has UserRole.VENDOR role
    - Redirect to /vendor/register if user lacks VENDOR role
    - Add console warning when access is denied due to missing role
    - Add returnUrl query parameter when redirecting to login
    - _Requirements: 14.1, 14.2, 14.4_
  
  - [ ]* 10.2 Write unit tests for VendorGuard
    - Test unauthenticated user redirects to /login
    - Test authenticated user without VENDOR role redirects to /vendor/register
    - Test authenticated user with VENDOR role is allowed access
    - Test returnUrl is preserved when redirecting to login
    - _Requirements: 14.1, 14.2, 14.4_

- [ ] 11. Checkpoint - Verify all components integration
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Final integration and manual verification
  - [ ] 12.1 Test complete registration flow end-to-end
    - Login as regular user without VENDOR role
    - Complete vendor registration form
    - Verify AuthResponse is returned with new tokens
    - Verify tokens are stored in browser storage
    - Verify automatic redirect to vendor dashboard
    - Verify vendor profile loads without 404 error
    - Verify analytics section loads without 400 error
    - _Requirements: 11.1, 12.1, 12.2, 13.1_
  
  - [ ] 12.2 Test error scenarios
    - Test registration with invalid data shows appropriate errors
    - Test dashboard access without VENDOR role redirects correctly
    - Test analytics with new vendor shows empty/zero data gracefully
    - Test proxy configuration routes requests correctly
    - _Requirements: 12.5, 13.5, 15.1, 15.2_

- [ ] 13. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- The implementation is organized in waves: backend role/token handling → frontend token handling → error handling improvements
- No property-based tests are included as this is a bug fix focusing on integration and authorization flow
- Manual E2E testing is included as a final integration task to verify the complete user journey

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2"] },
    { "id": 2, "tasks": ["2.3"] },
    { "id": 3, "tasks": ["4.1", "4.2"] },
    { "id": 4, "tasks": ["4.3", "5.1"] },
    { "id": 5, "tasks": ["5.2", "6.1", "7.1", "8.1", "9.1", "10.1"] },
    { "id": 6, "tasks": ["6.2", "7.2", "8.2", "9.2", "10.2"] },
    { "id": 7, "tasks": ["12.1", "12.2"] }
  ]
}
```

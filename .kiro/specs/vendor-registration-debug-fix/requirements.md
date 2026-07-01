# Requirements Document

## Introduction

This document defines the requirements for debugging and fixing the vendor registration form submission issue and post-registration dashboard access errors in the AI-Powered Traveling Management System. The vendor registration form is a multi-step Angular form that allows authenticated users to register as vendors by providing their business information. While the registration now successfully saves data to the database, users are experiencing API errors when redirected to the vendor dashboard after successful registration, preventing them from accessing their vendor profile and analytics.

## Glossary

- **Registration_Form**: The multi-step Angular reactive form component that collects vendor business information across three steps
- **Vendor_Service**: The Angular service that handles HTTP communication with the backend vendor registration API
- **Backend_API**: The Spring Boot REST endpoint at `/api/v1/vendor/register` that processes vendor registration requests
- **Auth_Guard**: Angular route guard that ensures users are authenticated before accessing protected routes
- **Payload**: The JSON object containing all form data sent from the frontend to the backend
- **Form_Validation**: Client-side validation rules applied to form fields using Angular validators
- **HTTP_Client**: Angular's HttpClient service used for making API requests
- **Error_Response**: The error object returned by the backend when a request fails
- **User_Context**: The authenticated user's session information including JWT token and user ID
- **Vendor_Dashboard**: The main dashboard component for vendors that displays profile information, analytics, and navigation
- **Vendor_Overview**: The overview page within the vendor dashboard showing analytics summary and wallet information
- **Vendor_Role**: The ROLE_VENDOR authority required to access vendor-specific endpoints
- **Vendor_Status**: The approval status of a vendor (PENDING_REVIEW, APPROVED, REJECTED, SUSPENDED)
- **Role_Assignment**: The process of granting VENDOR role to a user after successful vendor registration

## Requirements

### Requirement 1: Form Validation and Data Collection

**User Story:** As a user attempting to register as a vendor, I want the form to validate my input correctly, so that I can identify and fix any errors before submission.

#### Acceptance Criteria

1. WHEN a user fills out Step 1 fields (businessName, vendorType, registrationNumber, taxId), THE Registration_Form SHALL validate required fields before allowing progression to Step 2
2. WHEN a user fills out Step 2 fields (email, phone, websiteUrl, address fields), THE Registration_Form SHALL validate email format, phone number, and required address fields before allowing progression to Step 3
3. WHEN a user fills out Step 3 fields (description), THE Registration_Form SHALL allow progression to submit regardless of whether description is provided
4. WHEN any form field contains invalid data, THE Registration_Form SHALL display clear, specific error messages indicating what needs to be corrected
5. THE Registration_Form SHALL prevent form submission when required fields are missing or invalid

### Requirement 2: Payload Construction and API Communication

**User Story:** As a developer, I want to ensure the registration payload is correctly structured, so that the backend API can successfully process vendor registration requests.

#### Acceptance Criteria

1. WHEN a user submits the registration form, THE Vendor_Service SHALL construct a payload containing all fields from steps 1, 2, and 3
2. THE Vendor_Service SHALL send the payload to the Backend_API endpoint at `/api/v1/vendor/register` using HTTP POST
3. THE Vendor_Service SHALL include authentication headers (JWT token) with the API request
4. WHEN the vendorType field is included in the payload, THE Vendor_Service SHALL ensure it matches one of the valid enum values (HOTEL, TOUR_GUIDE, TRANSPORT)
5. THE Vendor_Service SHALL ensure the countryCode field is exactly 2 uppercase letters
6. THE Vendor_Service SHALL include all required fields (businessName, vendorType, email, phone, addressLine1, city, countryCode) in the payload

### Requirement 3: Authentication and Authorization

**User Story:** As a system administrator, I want to ensure only authenticated users can access the vendor registration form, so that we maintain security and link vendor profiles to user accounts.

#### Acceptance Criteria

1. WHEN an unauthenticated user attempts to access `/vendor/register`, THE Auth_Guard SHALL redirect them to the login page
2. WHEN an authenticated user accesses `/vendor/register`, THE Registration_Form SHALL load successfully
3. WHEN the Vendor_Service makes an API request, THE HTTP_Client SHALL automatically include the JWT token from the user's session
4. IF the JWT token is missing or expired, THE Backend_API SHALL return a 401 Unauthorized error
5. WHEN a 401 error occurs, THE Registration_Form SHALL display an appropriate error message and prompt the user to log in again

### Requirement 4: Error Handling and User Feedback

**User Story:** As a user, I want to receive clear feedback when registration fails, so that I can understand what went wrong and how to fix it.

#### Acceptance Criteria

1. WHEN the Backend_API returns an error response, THE Vendor_Service SHALL extract the error message from the response
2. WHEN a network error occurs (connection refused, timeout, CORS), THE Registration_Form SHALL display a user-friendly error message indicating connectivity issues
3. WHEN a validation error occurs (400 Bad Request), THE Registration_Form SHALL display the specific validation errors returned by the backend
4. WHEN a server error occurs (500 Internal Server Error), THE Registration_Form SHALL display a message asking the user to try again later
5. WHEN a duplicate registration error occurs (user already registered as vendor), THE Registration_Form SHALL display a clear message and redirect to the vendor dashboard
6. THE Registration_Form SHALL display error messages prominently at the top of the form with appropriate styling
7. WHEN an error occurs, THE Registration_Form SHALL remain on the current step to allow the user to review and correct their information

### Requirement 5: Success Handling and Navigation

**User Story:** As a user who successfully registers as a vendor, I want to receive confirmation and be guided to the next steps, so that I understand the approval process and can access my vendor dashboard.

#### Acceptance Criteria

1. WHEN the Backend_API returns a successful response (201 Created), THE Registration_Form SHALL display a success message
2. THE Registration_Form SHALL inform the user that their application is under review and will be processed within 24-48 hours
3. WHEN the success message is displayed, THE Registration_Form SHALL provide two action buttons: "Go to Dashboard" and "View Application"
4. WHEN the user clicks "Go to Dashboard", THE Registration_Form SHALL navigate to `/vendor/dashboard`
5. WHEN the user clicks "View Application", THE Registration_Form SHALL navigate to `/vendor/application-status`
6. THE Registration_Form SHALL automatically redirect to the vendor dashboard after 2 seconds of displaying the success message

### Requirement 6: Loading States and User Experience

**User Story:** As a user, I want to see visual feedback while my registration is being processed, so that I know the system is working and don't attempt to submit multiple times.

#### Acceptance Criteria

1. WHEN the user clicks the submit button, THE Registration_Form SHALL immediately display a loading indicator
2. WHILE the registration request is processing, THE Registration_Form SHALL disable the submit button to prevent duplicate submissions
3. THE Registration_Form SHALL display a spinner animation and "Submitting..." text on the submit button during processing
4. WHEN the request completes (success or error), THE Registration_Form SHALL hide the loading indicator and re-enable the submit button (unless successful)
5. THE Registration_Form SHALL display a progress bar showing the current step (1 of 3, 2 of 3, 3 of 3)

### Requirement 7: Debugging and Diagnostics

**User Story:** As a developer, I want comprehensive logging and error information, so that I can quickly diagnose and fix registration issues.

#### Acceptance Criteria

1. WHEN a registration attempt fails, THE Vendor_Service SHALL log the full error response to the browser console
2. THE Vendor_Service SHALL log the request payload before sending to help diagnose data issues
3. WHEN a validation error occurs, THE Registration_Form SHALL log which fields failed validation
4. THE Registration_Form SHALL log the current authentication state when an authorization error occurs
5. THE Vendor_Service SHALL include the HTTP status code in error messages for debugging

### Requirement 8: CORS and Network Configuration

**User Story:** As a developer, I want to ensure the frontend can successfully communicate with the backend API, so that vendor registration requests are not blocked by CORS or network configuration issues.

#### Acceptance Criteria

1. THE Backend_API SHALL include appropriate CORS headers to allow requests from the Angular frontend
2. WHEN the frontend makes a request to `/api/v1/vendor/register`, THE Backend_API SHALL accept OPTIONS preflight requests
3. THE Backend_API SHALL allow the Authorization header in CORS configuration
4. IF a CORS error occurs, THE Registration_Form SHALL display a clear message indicating a configuration issue
5. THE Vendor_Service SHALL use the correct base URL from the environment configuration

### Requirement 9: Field Mapping and Data Integrity

**User Story:** As a developer, I want to ensure all form fields map correctly to the backend DTO, so that no data is lost or mismatched during transmission.

#### Acceptance Criteria

1. THE Registration_Form SHALL name all form controls to exactly match the Backend_API DTO field names
2. WHEN constructing the payload, THE Vendor_Service SHALL not include undefined or null values for optional fields
3. THE Registration_Form SHALL ensure the vendorType value is sent as an uppercase enum value (HOTEL, TOUR_GUIDE, TRANSPORT)
4. THE Registration_Form SHALL trim whitespace from all text input fields before submission
5. THE Registration_Form SHALL validate that the countryCode is converted to uppercase before submission
6. WHEN the user selects a vendor type from the dropdown, THE Registration_Form SHALL store the raw enum value without additional transformation

### Requirement 10: Integration Testing and Verification

**User Story:** As a quality assurance engineer, I want to verify the complete registration flow works end-to-end, so that I can confirm the fix resolves the reported issue.

#### Acceptance Criteria

1. WHEN a valid registration form is submitted with all required fields, THE Backend_API SHALL successfully create a vendor profile
2. WHEN the registration is successful, THE Backend_API SHALL return a 201 Created status with the vendor profile data
3. THE Integration_Test SHALL verify that the authentication token is correctly included in the request headers
4. THE Integration_Test SHALL verify that invalid data (missing required fields, invalid email format) is properly rejected with appropriate error messages
5. THE Integration_Test SHALL verify that the vendor profile is linked to the authenticated user's account

### Requirement 11: Post-Registration Role Assignment

**User Story:** As a newly registered vendor, I want to be automatically assigned the VENDOR role upon successful registration, so that I can immediately access my vendor dashboard without encountering authorization errors.

#### Acceptance Criteria

1. WHEN the Backend_API successfully creates a vendor profile, THE Role_Assignment process SHALL automatically assign the VENDOR role to the user
2. THE Role_Assignment SHALL update the user's authorities in the authentication context
3. THE Backend_API SHALL include the updated role information in the registration response
4. WHEN the vendor registration is successful, THE Backend_API SHALL ensure the JWT token reflects the new VENDOR role
5. IF the user already has the VENDOR role, THE Role_Assignment process SHALL not create duplicate role assignments

### Requirement 11: Post-Registration Dashboard Access

**User Story:** As a newly registered vendor, I want to access my vendor dashboard immediately after registration without encountering 404 or 403 errors, so that I can start managing my vendor profile and services.

#### Acceptance Criteria

1. WHEN a newly registered vendor is redirected to `/vendor/dashboard`, THE Vendor_Dashboard SHALL successfully load without 404 errors
2. WHEN the Vendor_Dashboard calls GET `/api/v1/vendor/profile`, THE Backend_API SHALL return the vendor's profile information with status 200
3. WHEN the vendor has PENDING_REVIEW status, THE Vendor_Dashboard SHALL display appropriate messaging indicating the profile is under review
4. IF the user does not have the VENDOR role, THE Backend_API SHALL return 403 Forbidden with a clear error message
5. THE Vendor_Dashboard SHALL gracefully handle 403 errors by displaying a message and redirecting to the registration page

### Requirement 12: Analytics API Error Handling

**User Story:** As a newly registered vendor, I want the analytics section to handle missing data gracefully, so that I don't see error messages when no analytics data exists yet.

#### Acceptance Criteria

1. WHEN the Vendor_Overview calls GET `/api/v1/vendor/analytics/summary` with date parameters, THE Backend_API SHALL return 200 with empty or zero-filled analytics data if no bookings exist
2. THE Backend_API SHALL accept date query parameters in ISO 8601 format (YYYY-MM-DD)
3. IF the user does not have the VENDOR role, THE Backend_API SHALL return 403 Forbidden
4. IF the vendor profile does not exist, THE Backend_API SHALL return 404 Not Found
5. THE Vendor_Overview SHALL display a "No data available" message when analytics data is empty or zero
6. THE Vendor_Overview SHALL handle 400 Bad Request errors by logging the error and displaying a user-friendly message

### Requirement 13: Vendor Guard Authorization Check

**User Story:** As a system administrator, I want to ensure the VendorGuard properly validates vendor role and status, so that only authorized vendors can access vendor-specific routes.

#### Acceptance Criteria

1. WHEN a user attempts to access `/vendor/dashboard`, THE VendorGuard SHALL verify the user has the VENDOR role
2. IF the user does not have the VENDOR role, THE VendorGuard SHALL redirect to `/vendor/register`
3. WHEN the user has the VENDOR role but no vendor profile exists, THE VendorGuard SHALL allow access (the dashboard will handle the 404)
4. THE VendorGuard SHALL check role information from the current authentication context
5. THE VendorGuard SHALL not make additional API calls to verify vendor status (rely on role-based access control)

### Requirement 14: Environment and Proxy Configuration Verification

**User Story:** As a developer, I want to verify that the Angular environment configuration and proxy setup correctly route API requests to the Spring Boot backend, so that there are no base URL or CORS issues.

#### Acceptance Criteria

1. THE environment.apiUrl SHALL be set to `/api` to work with the development proxy
2. THE proxy.conf.json SHALL forward all `/api` requests to `http://localhost:8080` (the Spring Boot backend port)
3. THE Backend_API SHALL accept requests with the `/api` prefix
4. WHEN running `ng serve`, THE Angular development server SHALL use the proxy configuration
5. THE proxy configuration SHALL set `changeOrigin: true` to avoid CORS issues

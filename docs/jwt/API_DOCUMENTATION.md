# JWT Authentication API Documentation

**Version**: 1.0.0  
**Last Updated**: 2025-01-10  
**Status**: Production Ready

---

## Table of Contents

1. [Overview](#overview)
2. [Accessing the Documentation](#accessing-the-documentation)
3. [Authentication](#authentication)
4. [API Endpoints](#api-endpoints)
5. [Error Codes](#error-codes)
6. [Request/Response Examples](#requestresponse-examples)
7. [Testing with Swagger UI](#testing-with-swagger-ui)
8. [Configuration](#configuration)

---

## Overview

The JWT Authentication API provides secure, stateless authentication for the AI-Powered Travel Management System (APTMS). This API uses JSON Web Tokens (JWT) for authentication and authorization.

### Key Features

- **Stateless Authentication**: No server-side session storage required
- **Token-Based Access**: Bearer token authentication for all protected endpoints
- **Automatic Token Refresh**: Seamless token renewal without re-authentication
- **Multi-Device Support**: Manage sessions across multiple devices
- **Security Features**: Account lockout, token blacklisting, refresh token rotation

### Technology Stack

- **Framework**: Spring Boot 4.0.3
- **Security**: Spring Security 6.x with JWT
- **Documentation**: SpringDoc OpenAPI 3.0 (Swagger UI)
- **Token Library**: JJWT 0.12.5
- **Cache**: Redis for token blacklist

---

## Accessing the Documentation

### Interactive API Documentation (Swagger UI)

Once the application is running, access the interactive API documentation at:

**Local Development**:
```
http://localhost:8080/swagger-ui.html
```

**Production**:
```
https://api.aptms.com/swagger-ui.html
```

### OpenAPI Specification (JSON)

Download the raw OpenAPI specification:

```
http://localhost:8080/api-docs
```

### Features of Swagger UI

- **Try It Out**: Test API endpoints directly from the browser
- **Request/Response Examples**: See sample payloads for all endpoints
- **Schema Definitions**: View detailed data models
- **Authentication**: Authorize once and test all protected endpoints
- **Error Documentation**: Complete error code reference

---

## Authentication

### How to Authenticate

1. **Register or Login** to obtain tokens:
   - `POST /api/auth/register` - Register new user
   - `POST /api/auth/login` - Login existing user

2. **Extract Tokens** from the response:
   ```json
   {
     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
     "tokenType": "Bearer",
     "expiresIn": 900
   }
   ```

3. **Use Access Token** in Authorization header:
   ```
   Authorization: Bearer <access_token>
   ```

### Token Lifecycle

| Token Type | TTL | Storage | Purpose |
|------------|-----|---------|---------|
| Access Token | 15 minutes | Memory (frontend) | API authentication |
| Refresh Token | 7 days | sessionStorage | Token renewal |

### Authenticating in Swagger UI

1. Click the **"Authorize"** button (lock icon) at the top right
2. Enter your access token in the format: `Bearer <your_token>`
3. Click **"Authorize"**
4. All subsequent requests will include the token automatically

---

## API Endpoints

### Public Endpoints (No Authentication Required)

#### 1. Register New User
```
POST /api/auth/register
```

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "role": "USER"
}
```

**Response** (201 Created):
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Validation Rules**:
- Email: Valid format, unique
- Password: Minimum 8 characters
- Username: 3-50 characters
- Role: USER, ADMIN, or VENDOR

---

#### 2. Login User
```
POST /api/auth/login
```

**Request Body**:
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Security Features**:
- Account lockout after 5 failed attempts (15 minutes)
- Generic error messages to prevent account enumeration
- Failed attempt counter reset on successful login

---

#### 3. Refresh Access Token
```
POST /api/auth/refresh
```

**Request Body**:
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Response** (200 OK):
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "new-refresh-token-uuid",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

**Token Rotation**:
- Old refresh token is immediately invalidated
- New refresh token is issued
- Reuse detection triggers full session revocation

---

### Protected Endpoints (Authentication Required)

#### 4. Logout (Single Session)
```
POST /api/auth/logout
Authorization: Bearer <access_token>
```

**Response** (204 No Content)

**Actions Performed**:
- Access token's jti added to blacklist
- Refresh token deleted from database
- Current session terminated

---

#### 5. Logout All Sessions
```
POST /api/auth/logout-all
Authorization: Bearer <access_token>
```

**Response** (204 No Content)

**Actions Performed**:
- All refresh tokens for user deleted
- All active sessions terminated
- User must re-authenticate on all devices

**Use Cases**:
- Device lost or stolen
- Password change
- Suspicious activity detected

---

#### 6. Get Current User Profile
```
GET /api/auth/me
Authorization: Bearer <access_token>
```

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "createdAt": "2025-01-01T10:00:00Z",
  "lastLoginAt": "2025-06-01T12:00:00Z"
}
```

---

## Error Codes

All error responses follow this structure:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2025-06-01T12:00:00Z",
  "path": "/api/auth/endpoint"
}
```

### Authentication Error Codes

| Error Code | HTTP Status | Description | Action |
|------------|-------------|-------------|--------|
| `TOKEN_MISSING` | 401 | Missing Authorization header | Add Bearer token to header |
| `TOKEN_INVALID` | 401 | Invalid token signature or malformed JWT | Check token format and signature |
| `TOKEN_EXPIRED` | 401 | Token has expired | Use refresh token to get new access token |
| `TOKEN_REVOKED` | 401 | Token is blacklisted | Re-authenticate (token was logged out) |
| `REFRESH_TOKEN_REUSE_DETECTED` | 401 | Refresh token reused (security event) | Re-authenticate (all sessions revoked) |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password | Check credentials |
| `ACCOUNT_LOCKED` | 423 | Too many failed login attempts | Wait 15 minutes or contact support |
| `EMAIL_ALREADY_EXISTS` | 409 | Email already registered | Use different email or login |
| `VALIDATION_ERROR` | 400 | Invalid input data | Check request body format |

### Example Error Responses

**Token Expired**:
```json
{
  "error": "TOKEN_EXPIRED",
  "message": "Token has expired",
  "timestamp": "2025-06-01T12:00:00Z",
  "path": "/api/auth/me"
}
```

**Account Locked**:
```json
{
  "error": "ACCOUNT_LOCKED",
  "message": "Account locked due to too many failed login attempts. Try again after 15 minutes.",
  "timestamp": "2025-06-01T12:00:00Z",
  "path": "/api/auth/login",
  "retryAfter": "2025-06-01T12:15:00Z"
}
```

**Validation Error**:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid input data",
  "timestamp": "2025-06-01T12:00:00Z",
  "path": "/api/auth/register",
  "details": {
    "email": "Invalid email format",
    "password": "Password must be at least 8 characters"
  }
}
```

---

## Request/Response Examples

### Complete Authentication Flow

#### Step 1: Register New User

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "role": "USER"
  }'
```

**Response**:
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE3MTcyMDAwMDAsImV4cCI6MTcxNzIwMDkwMCwianRpIjoiN2M5ZTY2NzktNzQyNS00MGRlLTk0NGItZTA3ZmMxZjkwYWU3IiwiaXNzIjoiY29tLmFwdG1zLmF1dGgiLCJhdWQiOiJjb20uYXB0bXMuYXBpIiwicm9sZXMiOlsiVVNFUiJdLCJlbWFpbCI6ImpvaG5AZXhhbXBsZS5jb20ifQ.signature",
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### Step 2: Access Protected Endpoint

**Request**:
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["USER"],
  "createdAt": "2025-01-01T10:00:00Z",
  "lastLoginAt": "2025-06-01T12:00:00Z"
}
```

#### Step 3: Refresh Token (After 15 Minutes)

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }'
```

**Response**:
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["USER"]
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "new-refresh-token-uuid",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### Step 4: Logout

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Response**: 204 No Content

---

## Testing with Swagger UI

### Step-by-Step Guide

1. **Start the Application**
   ```bash
   mvn spring-boot:run
   ```

2. **Open Swagger UI**
   - Navigate to: `http://localhost:8080/swagger-ui.html`

3. **Register a Test User**
   - Expand `POST /api/auth/register`
   - Click **"Try it out"**
   - Fill in the request body:
     ```json
     {
       "username": "testuser",
       "email": "test@example.com",
       "password": "TestPass123!",
       "role": "USER"
     }
     ```
   - Click **"Execute"**
   - Copy the `accessToken` from the response

4. **Authorize Swagger UI**
   - Click the **"Authorize"** button (lock icon) at the top
   - Paste your token in the format: `Bearer <your_access_token>`
   - Click **"Authorize"**
   - Click **"Close"**

5. **Test Protected Endpoints**
   - Expand `GET /api/auth/me`
   - Click **"Try it out"**
   - Click **"Execute"**
   - You should see your user profile in the response

6. **Test Token Refresh**
   - Expand `POST /api/auth/refresh`
   - Click **"Try it out"**
   - Enter your refresh token from the registration response
   - Click **"Execute"**
   - You should receive new tokens

7. **Test Logout**
   - Expand `POST /api/auth/logout`
   - Click **"Try it out"**
   - Click **"Execute"**
   - You should receive a 204 No Content response

---

## Configuration

### Environment Variables

Configure the JWT authentication system using these environment variables:

```bash
# JWT Configuration
JWT_SECRET=your-256-bit-secret-minimum-32-characters-long
JWT_ACCESS_TOKEN_TTL=900000          # 15 minutes in milliseconds
JWT_REFRESH_TOKEN_TTL=604800000      # 7 days in milliseconds
JWT_ISSUER=com.aptms.auth
JWT_AUDIENCE=com.aptms.api
JWT_ALGORITHM=HS256

# Redis Configuration (for token blacklist)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Security Configuration
MAX_FAILED_ATTEMPTS=5
LOCKOUT_DURATION_MINUTES=15
PASSWORD_MIN_LENGTH=8
```

### Application Properties

The following properties are configured in `application.properties`:

```properties
# OpenAPI/Swagger Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
```

### Customizing Swagger UI

To customize the Swagger UI appearance or behavior, modify `OpenApiConfig.java`:

```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Your Custom Title")
                .version("2.0.0")
                .description("Your custom description"))
            // ... additional configuration
    }
}
```

---

## Additional Resources

### Related Documentation

- [Migration Guide](./MIGRATION_GUIDE.md) - Step-by-step migration from session-based auth
- [Requirements Document](../../.kiro/specs/jwt-authentication/requirements.md) - Detailed requirements
- [Design Document](../../.kiro/specs/jwt-authentication/design.md) - Technical design and architecture

### External References

- [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
- [RFC 6750 - OAuth 2.0 Bearer Token Usage](https://tools.ietf.org/html/rfc6750)
- [OWASP JWT Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [SpringDoc OpenAPI Documentation](https://springdoc.org/)

### Support

For questions or issues:
- **Email**: dev-team@aptms.com
- **Documentation**: http://localhost:8080/swagger-ui.html
- **Issue Tracker**: [GitHub Issues](https://github.com/aptms/aptms/issues)

---

**Last Updated**: 2025-01-10  
**Version**: 1.0.0  
**Status**: Production Ready

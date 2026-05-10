# Configuration Management Guide

## Overview

The AI-Powered Travel Management System uses a comprehensive configuration management approach with support for multiple environments and externalized configuration.

## Configuration Structure

### 1. Configuration Properties Classes

Located in `src/main/java/aptms/config/properties/`:

- **ApplicationProperties**: General application settings
- **DatabaseProperties**: Database connection and JPA settings
- **SecurityProperties**: Security, JWT, CORS, and password policies

### 2. Constants Classes

Located in `src/main/java/aptms/constants/`:

- **SecurityConstants**: Security-related constants (roles, messages)
- **BookingConstants**: Booking status and messages
- **EntityConstants**: Entity names and status values
- **ValidationConstants**: Validation messages and constraints

### 3. Enums

Located in `src/main/java/aptms/enums/`:

- **UserRole**: USER, ADMIN, VENDOR
- **BookingStatus**: PENDING, CONFIRMED, CANCELLED, etc.
- **HotelStatus**: ACTIVE, INACTIVE, MAINTENANCE
- **RoomStatus**: AVAILABLE, BOOKED, MAINTENANCE, UNAVAILABLE

## Environment Configuration

### Development Environment

```bash
# Set active profile
export SPRING_PROFILES_ACTIVE=dev

# Or in application.properties
spring.profiles.active=dev
```

**Features:**
- Verbose logging
- SQL query logging enabled
- Relaxed CORS policy
- H2 console enabled (if configured)

### Production Environment

```bash
# Set active profile
export SPRING_PROFILES_ACTIVE=prod

# Required environment variables
export DB_URL=jdbc:mysql://production-host:3306/travel_db
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export JWT_SECRET=your_production_jwt_secret
export CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

**Features:**
- Minimal logging
- SQL query logging disabled
- Strict CORS policy
- Database validation only (no auto-update)
- Connection pooling optimized

### Test Environment

```bash
# Set active profile
export SPRING_PROFILES_ACTIVE=test
```

**Features:**
- H2 in-memory database
- Database recreated for each test
- Fast execution

## Environment Variables

### Required Variables (Production)

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | Database connection URL | `jdbc:mysql://localhost:3306/travel_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `secure_password` |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | `your_secret_key` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `https://yourdomain.com` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8080` |
| `JPA_DDL_AUTO` | Hibernate DDL mode | `update` |
| `JWT_EXPIRATION` | JWT expiration in ms | `86400000` (24h) |
| `PASSWORD_STRENGTH` | BCrypt strength | `10` |
| `DEFAULT_PAGE_SIZE` | Default pagination size | `20` |
| `LOG_LEVEL` | Root log level | `INFO` |

## Using Environment Variables

### Method 1: .env File (Development)

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your values:
   ```properties
   DB_PASSWORD=my_local_password
   JWT_SECRET=my_dev_secret
   ```

3. Load environment variables:
   ```bash
   # Linux/Mac
   export $(cat .env | xargs)
   
   # Or use a tool like direnv
   ```

### Method 2: IDE Configuration

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Environment Variables → Add
3. Add each variable

**VS Code:**
1. Create `.vscode/launch.json`
2. Add environment variables in `env` section

### Method 3: System Environment Variables

```bash
# Linux/Mac
export DB_PASSWORD=my_password

# Windows
set DB_PASSWORD=my_password
```

### Method 4: Docker/Kubernetes

**Docker Compose:**
```yaml
services:
  app:
    environment:
      - DB_URL=jdbc:mysql://db:3306/travel_db
      - DB_USERNAME=root
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
```

**Kubernetes:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  db-password: <base64-encoded>
  jwt-secret: <base64-encoded>
```

## Configuration Best Practices

### 1. Never Commit Secrets

❌ **Don't:**
```properties
spring.datasource.password=mypassword123
```

✅ **Do:**
```properties
spring.datasource.password=${DB_PASSWORD}
```

### 2. Use Strong JWT Secrets

Generate a strong secret:
```bash
# Generate 64-byte random secret
openssl rand -base64 64
```

### 3. Environment-Specific Settings

- **Development**: Verbose logging, relaxed security
- **Production**: Minimal logging, strict security
- **Test**: In-memory database, fast execution

### 4. Validate Configuration on Startup

The application validates required configuration on startup. Missing required values will prevent startup with clear error messages.

## Accessing Configuration in Code

### Using @Value Annotation

```java
@Value("${app.name}")
private String appName;
```

### Using Configuration Properties

```java
@Service
public class MyService {
    private final ApplicationProperties appProperties;
    
    public MyService(ApplicationProperties appProperties) {
        this.appProperties = appProperties;
    }
    
    public void doSomething() {
        String version = appProperties.getVersion();
    }
}
```

### Using Constants

```java
import static aptms.constants.SecurityConstants.*;

if (role.equals(ROLE_ADMIN)) {
    // Admin logic
}
```

### Using Enums

```java
BookingStatus status = BookingStatus.CONFIRMED;
String statusValue = status.getValue(); // "CONFIRMED"
```

## Troubleshooting

### Issue: Application won't start

**Check:**
1. Required environment variables are set
2. Database is accessible
3. Profile is correctly set

### Issue: Configuration not loading

**Check:**
1. File name: `application-{profile}.properties`
2. Active profile: `spring.profiles.active=dev`
3. Property syntax: `key=value` (no spaces around `=`)

### Issue: Environment variables not working

**Check:**
1. Variables are exported in current shell
2. IDE is restarted after setting variables
3. Syntax: `${VAR_NAME:default_value}`

## Migration from Hard-Coded Values

### Before (Hard-Coded):
```java
String currentLoggingRole = "USER"; // Hard-coded
```

### After (Configuration):
```java
import static aptms.constants.SecurityConstants.ROLE_USER;

String currentLoggingRole = ROLE_USER; // From constants
```

### Before (Magic Strings):
```java
if (status.equals("CANCELLED")) { ... }
```

### After (Enums):
```java
if (status == BookingStatus.CANCELLED) { ... }
```

## Additional Resources

- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)

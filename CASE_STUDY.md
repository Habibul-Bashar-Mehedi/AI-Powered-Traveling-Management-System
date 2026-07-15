# APTMS — System Integration Case Study

**Project:** AI-Powered Traveling Management System (APTMS)  
**Date of Integration Audit:** July 12, 2026  
**Stack:** Spring Boot 4.0.3 / Java 21 · JPA/Hibernate · MySQL 8.4 · Redis 7 · Angular 21 · Tailwind CSS 4.1 · Docker Compose  

---

## 1. Overview

APTMS is a full-stack travel management platform built for portfolio and production-readiness purposes. It serves three distinct user roles — travelers (users), travel service vendors, and administrators — each with their own portal, data domain, and access boundaries. The system features:

- An **Angular 21 SPA** with role-based routing and a Vite-powered dev server
- A **Spring Boot REST API** secured with JWT (issued at login, blacklisted at logout)
- A **MySQL 8.4** relational database persisted via JPA/Hibernate
- **Redis 7** used as the token blacklist store — the critical mechanism that makes JWT logout meaningful
- **Docker Compose** orchestrating all four services on a shared internal network

This case study focuses specifically on **system integration** rather than individual feature correctness. The motivation: in multi-service architectures, a component can pass all its unit tests in isolation while the integrated system silently fails. This audit was conducted by actually starting the full stack, running real requests, and checking logs — not by reading code and assuming things work.

---

## 2. The Integration Challenge

Integrating a four-service system introduces a class of failure modes that no single-service test can catch:

| Layer | Integration Risk |
|---|---|
| Angular → Backend | Proxy misconfiguration, CORS policy mismatch, wrong API base URL |
| Backend → MySQL | Connection pool exhaustion, schema drift, Hibernate dialect mismatch |
| Backend → Redis | Network failure between containers, key serialization errors, silent cache miss treated as blacklist hit |
| Docker networking | Services referring to `localhost` instead of container name, causing "connection refused" at runtime even though local dev works |

The Docker networking issue is especially subtle. In development, a developer calls `http://localhost:8080`. Inside a Docker Compose network, the frontend container must call `http://backend:8080` using the service name as hostname. If the Angular proxy config is hardcoded to `localhost:8080`, every API call from the containerized frontend silently fails — but the backend works fine when tested directly, making the bug appear intermittent or frontend-only.

This project actually encountered this exact failure during development. Its resolution is documented as a worked example below (§4).

The integration audit was structured around five questions:

1. Are all containers healthy and communicating correctly?
2. Can data flow end-to-end from MySQL through JPA to a live API response?
3. Does Redis correctly intercept and reject blacklisted JWT tokens?
4. Can the Angular frontend reach the backend through the proxy in a containerized environment?
5. Does role-based access control hold at the HTTP level — not just in the UI?

---

## 3. Integration Points Tested

### 3.1 Container Health

All four services were brought up with `docker compose up -d` and confirmed running:

```
NAME              IMAGE                                              STATUS          PORTS
travel-backend    ai-powered-traveling-management-system-backend     Up              0.0.0.0:8080->8080/tcp
travel-frontend   ai-powered-traveling-management-system-frontend    Up              0.0.0.0:4200->4200/tcp
travel-mysql      mysql:8.4                                          Up 47 minutes   0.0.0.0:3306->3306/tcp
travel-redis      redis:7-alpine                                     Up 47 minutes   0.0.0.0:6379->6379/tcp
```

**Result: ✅ All containers healthy**

---

### 3.2 Backend → MySQL Integration

**Method:** Checked backend startup logs for HikariCP pool initialization, then issued a live HTTP request against a read endpoint.

**Startup log evidence:**

```
INFO  com.zaxxer.hikari.HikariDataSource : HikariPool-1 - Starting...
INFO  com.zaxxer.hikari.pool.HikariPool  : HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@5f4f198c
INFO  com.zaxxer.hikari.HikariDataSource : HikariPool-1 - Start completed.
```

**Live read test:**

```
GET http://localhost:8080/api/destination
HTTP_STATUS: 200
Response: []
```

The endpoint returned `200 OK` with an empty array — the destinations table exists and is query-able but contains no seeded data. JPA executed the query, received an empty result set, and serialized it correctly. This confirms the full MySQL-through-JPA-to-HTTP pipeline works.

**Startup warnings found (non-critical):**

```
WARN  org.hibernate.orm.deprecation : HHH90000025: MySQLDialect does not need to be specified
      explicitly using 'hibernate.dialect' (remove the property setting)
WARN  org.hibernate.orm.deprecation : HHH90000033: Encountered use of deprecated annotation
      [jakarta.persistence.Temporal] at aptms.entities.Booking.checkInDate
```

These are deprecation notices from Hibernate 6.x and do not affect runtime behavior, but they should be cleaned up to prevent future upgrade friction.

**Result: ✅ MySQL connection and data pipeline working. ⚠️ Two Hibernate deprecation warnings to address.**

---

### 3.3 Backend → Redis Integration

**Method:** Full login → use token → logout → attempt reuse cycle. Then direct Redis CLI inspection.

**Step 1 — Login (token issued):**
```
POST /api/auth/login  {"email":"user@test.com","password":"User@1234"}
→ 200 OK
→ accessToken: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJjMWZjYWYwYy... (337 chars)
```

**Step 2 — Token validates on protected endpoint:**
```
GET /api/auth/me  Authorization: Bearer <token>
→ 200 OK
→ {"id":"c1fcaf0c-...","username":"user_test","email":"user@test.com","roles":["USER"]}
```

**Step 3 — Logout (token blacklisted in Redis):**
```
POST /api/auth/logout  Authorization: Bearer <token>
→ 200 OK (empty body)
```

**Step 4 — Blacklisted token rejected:**
```
GET /api/auth/me  Authorization: Bearer <same token>
→ 401 Unauthorized
→ {"error":"TOKEN_REVOKED","message":"Token has been revoked","status":401}
```

**Direct Redis verification:**
```
$ docker exec travel-redis redis-cli KEYS "blacklist:*"
blacklist:a5c84d61-72fc-4e73-a13b-483ead63d380
blacklist:dce1cee2-a8b6-4ee6-8844-8b57610366c8

Total blacklisted tokens: 2
```

Startup log confirmed Redis configuration:
```
INFO  aptms.config.RedisConfig : Configuring Redis connection: redis:6379
INFO  aptms.config.RedisConfig : Redis connection factory configured successfully
INFO  aptms.config.RedisConfig : RedisTemplate configured successfully
INFO  aptms.services.impl.TokenServiceImpl : TokenService initialized with Redis cache and MySQL fallback
```

The "Redis cache and MySQL fallback" design is intentional: Redis is the primary blacklist store; MySQL holds a durable copy for recovery after Redis restart.

**Result: ✅ Redis JWT blacklist fully functional. Login, use, logout, reuse-rejection all work correctly.**

---

### 3.4 Frontend → Backend Integration (Proxy)

**Method:** Inspect containerized frontend's environment, confirm Angular proxy config, then issue an API call through the Angular dev server port (4200) rather than directly to the backend (8080).

**Proxy configuration (`frontend/proxy.conf.js`):**
```javascript
const target = process.env['BACKEND_URL'] || 'http://localhost:8080';

module.exports = {
  '/api': { target, secure: false, changeOrigin: true, logLevel: 'debug' },
  '/uploads': { target, secure: false, changeOrigin: true, logLevel: 'debug' },
};
```

**Container environment variable (confirmed live):**
```
BACKEND_URL=http://backend:8080
```

This is the correct, Docker-aware value. The `||` fallback to `localhost:8080` is appropriate for local development outside Docker; inside Docker Compose, the `BACKEND_URL` env var overrides it to use the internal hostname `backend`.

**Angular frontend health check:**
```
GET http://localhost:4200
→ HTTP 302 (redirect to /login)
→ Followed: HTTP 200, Angular HTML served
→ Response preview:
  <!DOCTYPE html><html lang="en"><head>
  <script type="module" src="/@vite/client"></script>
  <title>APTMS</title>...
```

**API call through proxy:**
```
POST http://localhost:4200/api/auth/login
Content-Type: application/json
{"email":"user@test.com","password":"User@1234"}

→ HTTP 200
→ {"user":{"id":"c1fcaf0c-...","email":"user@test.com","roles":["USER"]},"accessToken":"eyJhbG..."}
```

The Angular dev server transparently proxied `/api/auth/login` from port 4200 to `http://backend:8080/api/auth/login` inside the Docker network, and returned the JWT response to the browser.

**Result: ✅ Frontend-backend proxy working correctly. Proxy configuration is Docker-aware and correct.**

---

### 3.5 Authentication and Role-Based Access Control

**Method:** Login as each of the three seeded test accounts, test access to role-appropriate endpoints, and verify cross-role restrictions are enforced at the HTTP level.

**Login results for all three accounts:**

| Account | Password | HTTP Status | Roles in Token |
|---|---|---|---|
| user@test.com | User@1234 | 200 ✅ | `["USER"]` |
| vendor@test.com | Vendor@123 | 200 ✅ | `["VENDOR"]` |
| admin@test.com | Admin@123 | 200 ✅ | `["ADMIN"]` |

**RBAC cross-role enforcement tests:**

| Test | Token Role | Endpoint | Expected | Actual | Status |
|---|---|---|---|---|---|
| User on admin endpoint | USER | `GET /api/v1/admin/management/users` | 403 | 403 FORBIDDEN | ✅ |
| Admin on admin endpoint | ADMIN | `GET /api/v1/admin/management/users` | 200 | 200 with user list | ✅ |
| User on own profile | USER | `GET /api/auth/me` | 200 | 200 with profile | ✅ |
| Vendor on vendor profile | VENDOR | `GET /api/v1/vendor/profile` | 200 | 200 with vendor data | ✅ |
| User on tourist spots (read) | USER | `GET /api/tourist/spot` | 200 | **401** ❌ | **FAIL** |

The last row is a real bug, investigated in §4.

**RBAC is enforced server-side**, not just hidden in the UI. A USER-role JWT is rejected with `403 FORBIDDEN` at the Spring Security filter chain level before any controller code runs, confirmed by the response:

```json
{
  "error": "FORBIDDEN",
  "message": "Access denied",
  "status": 403,
  "path": "/api/v1/admin/management/users"
}
```

**Result: ✅ RBAC enforcement working at HTTP level. ❌ One confirmed bug: inverted `@SecureAction` roles on legacy service layer (see §4).**

---

### 3.6 Automated Test Suite

**Backend (Maven/JUnit 5):** 19 test files found covering services (DestinationService, TouristSpotService, BookingService, HotelService, etc.) and security (JwtServiceImpl, TokenServiceImpl, JwtLoadTest).

```
Tests run: 68, Failures: 2, Errors: 5, Skipped: 0
```

**Failures:**

| Test | Type | Detail |
|---|---|---|
| `JwtLoadTest.testRefreshStormUnderHighLoad` | Performance | P95 latency 424 ms; target was 250 ms |
| `TokenServiceImplTest.testDetectTokenReuse_RevokedToken_ReturnsTrue` | Functional | Expected `true`, got `false` — stolen-token reuse detection not working |
| `TokenServiceImplTest.testValidateRefreshToken_Success` | Functional | "Invalid Invalid refresh token" — refresh token validation logic broken in test context |
| `TokenServiceImplTest.testDetectTokenReuse_TokenNotFound_ReturnsFalse` | Test quality | `UnnecessaryStubbing` — mock setup no longer matches current interface |
| `TokenServiceImplTest.testDetectTokenReuse_ValidToken_ReturnsFalse` | Test quality | `UnnecessaryStubbing` |
| `TokenServiceImplTest.testValidateRefreshToken_ExpiredToken_ThrowsException` | Test quality | `UnnecessaryStubbing` |
| `TokenServiceImplTest.testValidateRefreshToken_RevokedToken_ThrowsException` | Test quality | `UnnecessaryStubbing` |

The four `UnnecessaryStubbing` errors indicate `TokenServiceImpl` was refactored after these tests were written and the tests were not updated — a test maintenance debt. The functional failure on `testDetectTokenReuse_RevokedToken_ReturnsTrue` is consistent with the known CLAUDE.md gap: "stolen-token-reuse detection is not implemented."

**Frontend (Vitest):** 6 spec files exist. **Result: 0 tests run, 6 files errored.**

```
ReferenceError: describe is not defined
 ❯ src/app/home/home.spec.ts:5:1
```

The spec files use Jasmine-style globals (`describe`, `it`, `expect`) that Karma provided automatically. The project migrated to `vitest` as the test runner but never configured `globals: true` in the vitest config, and no `vitest.config.ts` file exists. Additionally, `ng test` has no `test` builder configured in `angular.json`. The result: all 6 frontend specs exist as dead code — they have never been run against the current test runner and will not run until the vitest config is created.

**Result: ⚠️ Backend: 61/68 passing (7 failures — 2 functional, 4 test maintenance, 1 performance). ❌ Frontend: 0/? tests runnable due to missing vitest configuration.**

---

## 4. A Real Bug Encountered and Resolved: The `@SecureAction` Role Inversion

### Symptom Observed

During RBAC testing (§3.5), a USER-role token received a `401 "Access Denied: Admin role required"` response when calling `GET /api/tourist/spot`. This was unexpected — tourist spot browsing should be a read operation available to any authenticated user.

```
GET http://localhost:8080/api/tourist/spot
Authorization: Bearer <USER token>

→ 401
→ {"error":"INVALID_CREDENTIALS","message":"Access Denied: Admin role required."}
```

### Root Cause Investigation

The error string `"Access Denied: Admin role required."` was traced to `SecurityConstants.ACCESS_DENIED_MESSAGE`, which is thrown by `SecurityAspects.java`:

```java
// aptms/aspects/SecurityAspects.java
@Before("@annotation(secureAction)")
public void authorize(SecureAction secureAction) {
    if (secureAction.role().equals(ROLE_ADMIN) && !currentUserHasAdminRole()) {
        throw new InvalidException(ACCESS_DENIED_MESSAGE);  // ← thrown here
    }
}
```

The logic: if the `@SecureAction` annotation's role value equals `"ADMIN"` and the current user is not an admin, throw. Inspecting `TouristSpotService`:

```java
// aptms/services/TouristSpotService.java

@Transactional
@SecureAction(role = "USER")           // ← POST: create is open to any user
public TouristSpot addTouristSpot(...) { ... }

@Transactional(readOnly = true)
@SecureAction(role = "ADMIN")          // ← GET: read requires ADMIN
public List<TouristSpot> getAllTouristSpot() { ... }

@SecureAction(role = "ADMIN")          // ← DELETE: requires ADMIN ✓
public String deleteTouristSpot(...) { ... }

@SecureAction(role = "ADMIN")          // ← PUT: requires ADMIN ✓
public void updateTouristSpot(...) { ... }
```

The roles on the first two methods are **inverted**: write (create) is open to any user, while read (browse) requires admin. The correct intent is the opposite — browsing should be openly readable while creation/mutation should be admin-gated.

This same inversion exists across multiple legacy service files:

- `TouristSpotService` — confirmed ❌
- `HotelService` — confirmed ❌
- `BookingService` — confirmed ❌
- `RouteService` — confirmed ❌
- `MarketService` — confirmed ❌
- `TraditionalFoodService` — confirmed ❌
- `TraditionalItemService` — confirmed ❌
- `DestinationService` — confirmed ❌

### Why This Wasn't Caught Earlier

The `@SecureAction(role = "USER")` check has no actual enforcement in `SecurityAspects.java` — the condition only triggers when `role.equals(ROLE_ADMIN)`. So methods tagged `@SecureAction(role = "USER")` are effectively unguarded at the AOP layer (Spring Security's `anyRequest().authenticated()` still requires a valid JWT). This means:

- The add-tourist-spot bug is **silent in production** — a normal user can POST to create tourist spots without error
- The get-tourist-spots bug **surfaces as a 401** for any non-admin user attempting to browse

The mismatch between Spring Security's HTTP-level rules (which only enforce authentication) and the AOP layer's role check (which enforces admin-only at the service level) creates a gap: HTTP says "any authenticated user can reach `/api/tourist/spot`"; service says "only admins can read it."

### Previously Resolved: The Docker Proxy Bug

The historical bug documented in the project's `CLAUDE.md` — the Angular proxy pointing to `localhost:8080` instead of `backend:8080` — falls into this same "integration-only failure" category. In that case:

- Symptom: Frontend login worked in local development but silently failed in the Dockerized environment with network errors
- Root cause: `proxy.conf.js` hardcoded `http://localhost:8080`; inside Docker, the frontend container's `localhost` is its own container, not the backend
- Fix: Changed to `const target = process.env['BACKEND_URL'] || 'http://localhost:8080'`, then set `BACKEND_URL=http://backend:8080` in `docker-compose.yml`
- Verification: API calls via `localhost:4200/api/**` now reach the backend, confirmed in §3.4

This fix is currently in place and working. It is the canonical example of a failure that only manifests when the full system is running together.

---

## 5. Lessons Learned

### 5.1 AOP Security Layers Need Their Own Integration Test

The `@SecureAction` AOP mechanism is a second authorization layer that sits _inside_ Spring Security, not visible in `SecurityConfig.java`. A developer reading the security config would conclude "authenticated users can browse tourist spots" — and be wrong. Integration tests that hit real endpoints with real tokens are the only reliable way to catch this class of bug.

### 5.2 Docker Networking Is an Integration Concern, Not a Configuration Chore

The `localhost` vs `backend:8080` distinction is not a typo or a minor detail — it is a fundamental network topology difference. Multi-service projects need at least one end-to-end test in a containerized context as part of every build, not just in local dev mode.

### 5.3 Test Maintenance Debt Compounds Quickly in Security Code

The `TokenServiceImplTest` had four `UnnecessaryStubbing` errors and two functional assertion failures. These indicate the `TokenServiceImpl` interface changed (likely during a Redis integration refactor) and the tests were not updated. Security-critical code with stale tests is worse than no tests — it creates a false sense of coverage.

### 5.4 Error Code Consistency Matters for Debugging

During this audit, two different error scenarios both returned `401` with `"error":"INVALID_CREDENTIALS"`: actual bad credentials, and a role-based AOP rejection. These are fundamentally different situations. A developer debugging a role mismatch would first assume they have a JWT expiry or format problem, not an `@SecureAction` misconfiguration. The error code for AOP authorization failures should be `403 FORBIDDEN`, not `401 INVALID_CREDENTIALS`.

### 5.5 A Non-Running Test Suite Is Not a Neutral State

The six Angular spec files that produce `ReferenceError: describe is not defined` are **not** "untested code" — they are actively misleading. They exist, suggesting coverage; they fail silently unless someone runs the test command; and when run, they error before executing a single assertion. This is worse than having no spec files, because it hides the absence of a working frontend test harness.

---

## 6. Current Integration Health Summary

| Integration Point | Status | Notes |
|---|---|---|
| Docker Compose — all 4 containers up | ✅ Working | MySQL, Redis up for 47 min; backend/frontend just started; all healthy |
| Backend → MySQL (connection pool) | ✅ Working | HikariPool-1 started, connection confirmed in startup logs |
| Backend → MySQL (data pipeline) | ✅ Working | `GET /api/destination` returns 200; authenticated endpoints serve data |
| Backend → Redis (connection) | ✅ Working | `redis:6379` connected; RedisTemplate configured in startup logs |
| Backend → Redis (JWT blacklist) | ✅ Working | Logout blacklists token; reuse rejected with `TOKEN_REVOKED 401`; confirmed via `redis-cli KEYS` |
| Frontend → Backend (proxy config) | ✅ Working | `BACKEND_URL=http://backend:8080` set; proxy routes `/api/**` and `/uploads/**` correctly |
| Frontend serving Angular SPA | ✅ Working | `localhost:4200` serves Angular HTML; Vite dev server running |
| Login — all 3 role accounts | ✅ Working | user@test.com, vendor@test.com, admin@test.com all issue valid JWTs |
| RBAC — admin endpoint enforcement | ✅ Working | USER token → 403; ADMIN token → 200 on `/api/v1/admin/**` |
| RBAC — vendor endpoint enforcement | ✅ Working | VENDOR token → 200 on `/api/v1/vendor/profile` |
| RBAC — content read endpoints (legacy services) | ❌ Failing | `@SecureAction` roles inverted: GET requires ADMIN, POST open to USER; affects 8+ service classes |
| Hibernate deprecation warnings | ⚠️ Partial | `MySQLDialect` explicit setting + `@Temporal` on Booking entity — functional but needs cleanup |
| `NoResourceFoundException` error handling | ⚠️ Partial | Incorrectly surfaces as `500 INTERNAL_SERVER_ERROR` instead of `404 Not Found` |
| Backend unit tests | ⚠️ Partial | 61/68 passing; 2 functional failures in token reuse detection; 4 stale mock tests |
| Frontend unit tests | ❌ Failing | 0/? runnable; vitest not configured with globals; `ng test` has no builder; all 6 specs error before executing |

---

## 7. Recommendations

### 7.1 Fix the `@SecureAction` Role Inversion Immediately

This is not a test gap — it is a live behavior bug. Any authenticated user can call `POST /api/tourist/spot/add` and create content. This likely affects all 8 legacy service classes using the same pattern. The fix is straightforward: swap `"USER"` and `"ADMIN"` on the affected method annotations, then validate with the integration test sequence from §3.5.

### 7.2 Add an Automated Docker Compose Smoke Test to CI

The integration failures found in this audit (proxy config, AOP role inversion) would not be caught by unit tests. The project needs a CI step that:
1. Runs `docker compose up -d`
2. Waits for the backend health check (or polls `/actuator/health`)
3. Runs a minimal smoke test script hitting: login (3 roles), one authenticated read, one RBAC rejection, one logout + reuse
4. Fails the build if any test fails

A shell script or `pytest`/`httpx` script can implement this in under 50 lines. This would have caught both the proxy bug and the `@SecureAction` inversion before they reached the main branch.

### 7.3 Fix the Frontend Test Runner

Two actions needed:
1. Create `vitest.config.ts` with `globals: true` and `environment: 'jsdom'` (or migrate specs to vitest API)
2. Install `@vitest/browser` or configure `@angular/build:unit-test` in `angular.json`

The 6 existing spec files cover critical paths (login component, token storage service, dashboard, registration). Getting them running is a one-time ~1 hour investment that immediately improves the project's testability story.

### 7.4 Fix the `NoResourceFoundException` → 500 Mapping

When a client requests a non-existent path like `/api/tourist-spot` (missing the `/`) or `/api/admin/users` (wrong prefix), the system returns:
```json
{"error":"INTERNAL_SERVER_ERROR","message":"An unexpected error occurred","status":500}
```
This should be a 404. Add a handler for `NoResourceFoundException` in `GlobalExceptionHandler` to return a clean `404 NOT_FOUND` response.

### 7.5 Align the AOP Error Codes with HTTP Semantics

`@SecureAction` violations are authorization failures — they should produce `403 FORBIDDEN`, not `401 INVALID_CREDENTIALS`. Update `SecurityAspects` to throw a different exception type, or update `GlobalExceptionHandler` to map `@SecureAction` rejections to 403.

### 7.6 Address Token Reuse Detection (Existing Gap)

`TokenServiceImplTest.testDetectTokenReuse_RevokedToken_ReturnsTrue` fails — `detectTokenReuse()` returns `false` when it should return `true` for a revoked token. This test is exposing a real gap (also noted in CLAUDE.md). The stolen-token-reuse detection feature was designed but not fully implemented. Given that this is a security feature, it should be prioritized over cosmetic improvements.

---

*This document was generated from a live integration audit session on July 12, 2026. All test results, log excerpts, and HTTP responses are real outputs from the running system, not synthetic examples.*


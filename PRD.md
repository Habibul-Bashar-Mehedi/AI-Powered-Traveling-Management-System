# Product Requirements Document (PRD)
## AI-Powered Traveling Management System (APTMS)

**Version:** 1.0  
**Date:** July 12, 2026  
**Status:** Active Development  
**Owner:** Habibul Bashar Mehedi  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Product Vision & Goals](#2-product-vision--goals)
3. [Target Users & Personas](#3-target-users--personas)
4. [System Architecture Overview](#4-system-architecture-overview)
5. [Technology Stack](#5-technology-stack)
6. [Feature Requirements](#6-feature-requirements)
   - 6.1 [Authentication & Account Management](#61-authentication--account-management)
   - 6.2 [User Dashboard](#62-user-dashboard)
   - 6.3 [Hotel & Room Booking](#63-hotel--room-booking)
   - 6.4 [Vendor Onboarding & Management](#64-vendor-onboarding--management)
   - 6.5 [Vendor Service Management](#65-vendor-service-management)
   - 6.6 [Vendor Booking Management](#66-vendor-booking-management)
   - 6.7 [Vendor Wallet & Payouts](#67-vendor-wallet--payouts)
   - 6.8 [Vendor Analytics Dashboard](#68-vendor-analytics-dashboard)
   - 6.9 [Service Catalog & Discovery](#69-service-catalog--discovery)
   - 6.10 [Travel Content (Destinations, Spots, Food, Markets)](#610-travel-content)
   - 6.11 [AI-Powered Chat Assistant](#611-ai-powered-chat-assistant)
   - 6.12 [Admin Dashboard](#612-admin-dashboard)
   - 6.13 [Banner & Marketing Management](#613-banner--marketing-management)
7. [Security Requirements](#7-security-requirements)
8. [Data Model Summary](#8-data-model-summary)
9. [API Design Principles](#9-api-design-principles)
10. [Non-Functional Requirements](#10-non-functional-requirements)
11. [Role & Permission Matrix](#11-role--permission-matrix)
12. [Current Status & Completion](#12-current-status--completion)
13. [Known Gaps & Future Work](#13-known-gaps--future-work)

---

## 1. Executive Summary

APTMS is a full-stack, AI-augmented travel marketplace that connects **travelers** with **vendors** (hotels, tour guides, transport providers) through a single platform. It provides:

- A **traveler-facing** booking experience with AI travel assistance.
- A **vendor portal** for managing services, bookings, earnings, and analytics.
- An **admin back-office** for approvals, moderation, and system configuration.

The backend is a Spring Boot 4.0.3 REST API secured with JWT and RBAC. The frontend is an Angular 21 SPA styled with Tailwind CSS 4.1.

---

## 2. Product Vision & Goals

### Vision
Become the go-to AI-powered travel management platform for Southeast Asian destinations, enabling seamless discovery, booking, and management of travel services.

### Primary Goals
| # | Goal | Metric |
|---|------|--------|
| G1 | Enable travelers to search, discover, and book travel services end-to-end | Booking completion rate |
| G2 | Provide vendors a self-serve portal to list services and manage revenue | Vendor activation rate |
| G3 | Automate the vendor onboarding/approval pipeline for admins | Time-to-approval |
| G4 | Deliver AI-assisted travel recommendations via chat | Chat engagement rate |
| G5 | Maintain a secure, production-grade authentication and authorization system | Zero auth-related incidents |

---

## 3. Target Users & Personas

### 3.1 Traveler (Role: `USER`)
- **Description:** A consumer looking to browse destinations, book hotel rooms, and discover tours or transport services.
- **Key Needs:** Search services, compare options, book with confidence, get AI recommendations.
- **Pain Points:** Information fragmentation across sites, opaque pricing, lack of local knowledge.

### 3.2 Vendor (Role: `VENDOR`)
- **Description:** A business owner (hotel, tour guide, transport operator) who lists services on the platform.
- **Key Needs:** List and manage services, accept/reject bookings, track earnings, request payouts.
- **Pain Points:** Manual booking management, delayed payments, no centralized analytics.

### 3.3 Platform Administrator (Role: `ADMIN`)
- **Description:** An internal operator who moderates the platform, approves vendors, manages content, and monitors system health.
- **Key Needs:** Approve/reject vendor applications, manage users, configure banners, view system-wide analytics.
- **Pain Points:** Manual review queues, lack of audit trails.

### 3.4 Super Administrator (Role: `SUPER_ADMIN`)
- **Description:** Reserved for elevated platform-level access (e.g., system settings, commission configuration).
- **Status:** Role defined; feature scope planned for future phases.

---

## 4. System Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   Angular 21 SPA                    │
│        (Tailwind CSS · Reactive Forms · RxJS)       │
│  ┌──────────┐ ┌──────────┐ ┌────────┐ ┌─────────┐  │
│  │  Public  │ │  User    │ │ Vendor │ │  Admin  │  │
│  │  Pages   │ │Dashboard │ │ Portal │ │  Panel  │  │
│  └──────────┘ └──────────┘ └────────┘ └─────────┘  │
└────────────────────────┬────────────────────────────┘
                         │ HTTP/REST + JWT
┌────────────────────────▼────────────────────────────┐
│            Spring Boot 4.0.3 REST API               │
│  ┌─────────────┐  ┌───────────┐  ┌──────────────┐  │
│  │  Auth API   │  │  Booking  │  │  Vendor API  │  │
│  │  /api/auth  │  │    API    │  │ /api/v1/     │  │
│  └─────────────┘  └───────────┘  └──────────────┘  │
│  ┌─────────────┐  ┌───────────┐  ┌──────────────┐  │
│  │ Content API │  │  AI Chat  │  │  Admin API   │  │
│  │ /api/...    │  │  /api/ai  │  │ /api/v1/admin│  │
│  └─────────────┘  └───────────┘  └──────────────┘  │
│                Spring Security + JWT                │
└───────────┬──────────────────────┬──────────────────┘
            │                      │
     ┌──────▼──────┐      ┌────────▼────────┐
     │   MySQL 8   │      │  Redis 6         │
     │  (Primary   │      │  (Token Blacklist│
     │   Storage)  │      │   + Cache)       │
     └─────────────┘      └─────────────────┘
```

**Key design decisions:**
- Stateless JWT authentication (no server-side sessions).
- UUID primary keys on all sensitive/vendor entities for obfuscation.
- Soft-deletes on User, Vendor, and audit-sensitive entities (using `deleted_at` timestamp).
- Optimistic locking (`@Version`) on bookings and wallet transactions to prevent race conditions.
- Structured JSON logging (Logstash-compatible) for all security and business events.

---

## 5. Technology Stack

### Backend
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 4.0.3 |
| Language | Java | 21 (LTS) |
| ORM | Hibernate / Spring Data JPA | 7.2 / JPA 3.1 |
| Database | MySQL | 8.0+ |
| Security | Spring Security + JJWT | 6.x / 0.12.5 |
| Cache / Token Store | Redis | 6.0+ |
| Auditing | Hibernate Envers | (bundled) |
| Validation | Jakarta Validation | 3.0 |
| Logging | Logback + Logstash Encoder | — |
| Metrics | Micrometer + Prometheus | — |
| API Docs | SpringDoc OpenAPI | 2.3.0 |
| Boilerplate | Lombok | — |
| AI Integration | Google Gemini API | (via REST) |

### Frontend
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Angular | 21.2.0 |
| Language | TypeScript | 5.9.2 |
| Styling | Tailwind CSS | 4.1.12 |
| Reactivity | RxJS | 7.8.0 |
| Forms | Angular Reactive Forms | — |
| Routing | Angular Router (lazy-loaded) | — |
| SSR | @angular/ssr | 21.2.5 |
| HTTP | Angular HttpClient | — |

### Infrastructure
| Component | Technology |
|-----------|-----------|
| Containerization | Docker + Docker Compose |
| Database | MySQL 8 container |
| Cache | Redis 6 container |

---

## 6. Feature Requirements

---

### 6.1 Authentication & Account Management

#### FR-AUTH-001 — User Registration
- **Endpoint:** `POST /api/auth/register`
- **Access:** Public
- **Input:** `username`, `email`, `password`, `role`, `countryId`
- **Behavior:**
  - Validate all inputs (non-null, valid email format, password length ≥ 8).
  - Block registration from prohibited email domains (currently: `hotmail.com`, `email.com`, `test.com`).
  - Reject duplicate email with `409 Conflict`.
  - Encode password with BCrypt (strength 10).
  - Return JWT access token + refresh token on success.
- **Security:** Throws `InvalidException` (→ 401) for blocked domain; `DuplicateValueFoundExceptions` (→ 409) for duplicate email.

#### FR-AUTH-002 — User Login
- **Endpoint:** `POST /api/auth/login`
- **Access:** Public
- **Behavior:**
  - Lookup user by email (active users only — soft-deleted users rejected).
  - Check account lock status; reject with lockout expiry message if locked.
  - Verify BCrypt password; increment `failedLoginAttempts` on failure.
  - Lock account after 5 failed attempts (15-minute lockout).
  - On success: reset failed attempts, record `lastLoginAt`, issue access + refresh tokens.

#### FR-AUTH-003 — Token Refresh
- **Endpoint:** `POST /api/auth/refresh`
- **Behavior:** Validate refresh token, issue new access token. Rotate refresh token.

#### FR-AUTH-004 — Logout
- **Endpoint:** `POST /api/auth/logout`
- **Behavior:** Blacklist current access token in Redis until its expiration time. Invalidate refresh token.

#### FR-AUTH-005 — Account Lockout
- Triggered after `MAX_FAILED_ATTEMPTS` (5) consecutive failed logins.
- Lockout duration: `LOCKOUT_DURATION_MINUTES` (15 minutes), stored as `lockoutUntil` timestamp.
- Auto-unlocked when timestamp expires.

#### FR-AUTH-006 — Password Reset *(Planned)*
- Email-based password reset flow not yet implemented.

---

### 6.2 User Dashboard

- **Route:** `/dashboard` (requires `AuthGuard`)
- **Features:**
  - Overview of the user's active and past bookings.
  - Access to AI chat assistant.
  - Quick links to service catalog.

---

### 6.3 Hotel & Room Booking

#### FR-BOOK-001 — Hotel Listing
- **Endpoint:** `GET /api/hotel`
- **Access:** Mixed (public browse, authenticated to book)
- **Fields:** Name, address, contact, status (`ACTIVE` | `INACTIVE` | `MAINTENANCE`).

#### FR-BOOK-002 — Room Availability
- **Endpoint:** `GET /api/room?hotelId={id}`
- **Behavior:** Return rooms for a hotel. Filter by status = `AVAILABLE`.
- **Room fields:** Capacity, type, status, price.
- **Room statuses:** `AVAILABLE`, `BOOKED`, `MAINTENANCE`, `UNAVAILABLE`.

#### FR-BOOK-003 — Create Booking
- **Endpoint:** `POST /api/booking/add`
- **Access:** Authenticated (`USER`)
- **Input:** `roomId`, `hotelId`, `checkInDate`, `checkOutDate`, `guestCount`, `totalPrice`
- **Behavior:**
  - Validate room availability (status = `AVAILABLE`, capacity ≥ guestCount).
  - Create booking with status `PENDING`.
  - Update room status to `BOOKED`.

#### FR-BOOK-004 — Booking Lifecycle
| Status | Triggered By |
|--------|-------------|
| `PENDING` | Booking created |
| `CONFIRMED` | Vendor/admin confirms |
| `CHECKED_IN` | Check-in date reached |
| `CHECKED_OUT` | Check-out date reached |
| `COMPLETED` | Scheduled auto-completion job |
| `CANCELLED` | User or admin cancels |

#### FR-BOOK-005 — Booking Auto-Completion
- A scheduled job auto-completes bookings past their `checkOutDate`.
- On completion: credits vendor wallet with booking amount (minus platform commission).

---

### 6.4 Vendor Onboarding & Management

#### FR-VEND-001 — Vendor Registration
- **Endpoint:** `POST /api/v1/vendor/register`
- **Access:** Authenticated (`USER`)
- **Input:** `businessName`, `vendorType` (HOTEL | TOUR_GUIDE | TRANSPORT), `registrationId`, `taxId`, `address`, `contactEmail`, `contactPhone`, `description`, `logo`
- **Behavior:** Create vendor record with status `PENDING_REVIEW`. A user can only have one vendor account.

#### FR-VEND-002 — Document Upload
- Vendors upload supporting documents (Registration Certificate, Tax ID, Business License, Insurance, Other).
- Each document has an approval status (`PENDING`, `APPROVED`, `REJECTED`).

#### FR-VEND-003 — Vendor Status Lifecycle
| Status | Meaning |
|--------|---------|
| `PENDING_REVIEW` | Awaiting admin review |
| `APPROVED` | Active on platform |
| `REJECTED` | Application denied |
| `SUSPENDED` | Temporarily deactivated by admin |

#### FR-VEND-004 — Vendor Profile Management
- Approved vendors can update their profile (name, contact, logo, description).

---

### 6.5 Vendor Service Management

#### FR-VSVC-001 — Create Service
- **Endpoint:** `POST /api/v1/vendor/services`
- **Access:** `VENDOR`
- **Input:** `title`, `description`, `serviceType`, `price`, `pricingUnit`, `availabilitySchedule`, `maxCapacity`, `bookingMode`
- **Service types:** `HOTEL_ROOM`, `TOUR_PACKAGE`, `TRANSPORT_ROUTE`
- **Pricing units:** `PER_NIGHT`, `PER_PERSON`, `PER_SEAT`, `PER_TRIP`
- **Booking modes:** `INSTANT` (auto-confirm) | `MANUAL` (vendor confirms manually)

#### FR-VSVC-002 — Service Availability
- Vendors set date/time availability slots per service.
- Availability enforced during booking creation.

#### FR-VSVC-003 — Service Lifecycle
| Status | Meaning |
|--------|---------|
| `DRAFT` | Not yet published |
| `ACTIVE` | Visible in catalog |
| `INACTIVE` | Hidden from catalog |

#### FR-VSVC-004 — Service CRUD
- Vendors can edit service details, update availability, deactivate, and delete (soft) services.

---

### 6.6 Vendor Booking Management

#### FR-VBOOK-001 — Booking Inbox
- **Endpoint:** `GET /api/v1/vendor/bookings`
- **Access:** `VENDOR`
- **Behavior:** Return paginated list of bookings for the vendor's services. Filterable by status.

#### FR-VBOOK-002 — Confirm / Reject Booking
- **Endpoints:** `POST /api/v1/vendor/bookings/{id}/confirm` | `/reject`
- **Behavior:**
  - Confirm: moves booking to `CONFIRMED`, notifies user (future: push notification).
  - Reject: moves booking to `REJECTED`, releases service availability slot.

#### FR-VBOOK-003 — Booking Status Lifecycle (Vendor)
| Status | Triggered By |
|--------|-------------|
| `PENDING` | User creates booking |
| `CONFIRMED` | Vendor confirms |
| `REJECTED` | Vendor rejects |
| `COMPLETED` | Auto-scheduler on checkout |
| `CANCELLED` | User cancels before confirmation |

---

### 6.7 Vendor Wallet & Payouts

#### FR-WALL-001 — Wallet Summary
- **Endpoint:** `GET /api/v1/vendor/wallet`
- **Returns:** `totalEarnings`, `availableBalance`, `pendingBalance`, `totalPaidOut`, recent transactions.

#### FR-WALL-002 — Transaction History
- All credits (booking completions) and debits (payouts, refunds) recorded in `wallet_transaction`.
- Each transaction: `type` (CREDIT | DEBIT), `amount`, `description`, `createdAt`.

#### FR-WALL-003 — Payout Request
- **Endpoint:** `POST /api/v1/vendor/wallet/payout`
- **Input:** `amount`, `paymentMethod` (CREDIT_CARD | DEBIT_CARD | BANK_TRANSFER | WALLET)
- **Behavior:** Create `PayoutRequest` with status `PENDING`. Deduct from `pendingBalance`.
- Payout lifecycle: `PENDING` → `APPROVED` → `COMPLETED` | `REJECTED`

#### FR-WALL-004 — Platform Commission
- Configurable commission rate in `SystemSetting` entity (default: stored in DB).
- Deducted from vendor earnings on booking completion.

---

### 6.8 Vendor Analytics Dashboard

#### FR-ANA-001 — Revenue Overview
- **Endpoint:** `GET /api/v1/vendor/analytics/summary`
- **Returns:** Revenue for today, this week, this month, all-time.

#### FR-ANA-002 — Booking Trends
- **Endpoint:** `GET /api/v1/vendor/analytics/revenue`
- **Returns:** Time-series booking counts for chart visualization.

#### FR-ANA-003 — KPI Cards
- Active service count.
- Pending bookings count.
- Total confirmed bookings.
- Conversion rate (confirmed / total requests).

---

### 6.9 Service Catalog & Discovery

#### FR-CAT-001 — Browse Services
- **Endpoint:** `GET /api/catalog/services`
- **Access:** Public
- **Filter params:** `serviceType`, `vendorType`, `minPrice`, `maxPrice`, `location`, `keyword`
- **Returns:** Paginated list of active vendor services.

#### FR-CAT-002 — Service Detail
- Returns full service details including vendor profile, pricing, availability, booking mode.

---

### 6.10 Travel Content

All content endpoints follow the same CRUD pattern:

| Entity | Endpoint Base | Public GET | Admin/Vendor Write |
|--------|-------------|------------|-------------------|
| Tourist Spots | `/api/tourist-spot` | ✅ | ✅ |
| Destinations | `/api/destination` | ✅ | ✅ |
| Transport Routes | `/api/transport` | ✅ | ✅ |
| Traditional Foods | `/api/food` | ✅ | ✅ |
| Traditional Items | `/api/items` | ✅ | ✅ |
| Markets | `/api/market` | ✅ | ✅ |
| Travel Packages | `/api/package` | ✅ | ✅ |

Each entity supports: list all, get by ID, create, update, delete (soft where applicable). Images stored in `/uploads/` directory (disk-based, file path stored in DB).

---

### 6.11 AI-Powered Chat Assistant

#### FR-AI-001 — Send Chat Message
- **Endpoint:** `POST /api/ai/chat`
- **Access:** Authenticated
- **Input:** `message` (user's question or prompt)
- **Behavior:** Forward message + contextual travel data to Google Gemini API. Return AI-generated travel recommendation/response.

#### FR-AI-002 — Chat History
- **Endpoint:** `GET /api/ai/chat/history`
- **Behavior:** Return paginated chat history for the authenticated user.
- Chat messages persisted in `chat_history` table (`userId`, `message`, `role` USER|ASSISTANT, `timestamp`).

#### FR-AI-003 — Context Awareness *(Planned)*
- AI responses should be aware of user's booked destinations and travel dates.
- Currently: stateless per message; context injection planned.

---

### 6.12 Admin Dashboard

#### FR-ADM-001 — Vendor Approval Queue
- **Endpoint:** `GET /api/v1/admin/vendors/pending`
- **Access:** `ADMIN`
- **Behavior:** Return list of vendors with status `PENDING_REVIEW`. Admin can approve or reject.

#### FR-ADM-002 — Vendor Actions
- `POST /api/v1/admin/vendors/{id}/approve` — Set status to `APPROVED`.
- `POST /api/v1/admin/vendors/{id}/reject` — Set status to `REJECTED` (with reason).
- `POST /api/v1/admin/vendors/{id}/suspend` — Set status to `SUSPENDED`.

#### FR-ADM-003 — User Management
- `GET /api/v1/admin/management/users` — List all users (paginated).
- Admin can suspend/unsuspend users (soft delete via `deleted_at`).

#### FR-ADM-004 — Booking Analytics
- System-wide booking statistics (total, by status, by period).

#### FR-ADM-005 — System Settings
- Commission rate, platform fee, and other global config stored in `system_setting` table.

---

### 6.13 Banner & Marketing Management

#### FR-BAN-001 — Public Banners
- **Endpoint:** `GET /api/banner`
- **Access:** Public
- Returns active marketing banners (image URL, link, display order).

#### FR-BAN-002 — Banner CRUD (Admin)
- `POST /api/v1/admin/banners` — Create banner (upload image to `/uploads/banners/`).
- `PUT /api/v1/admin/banners/{id}` — Update banner.
- `DELETE /api/v1/admin/banners/{id}` — Deactivate/delete banner.

---

## 7. Security Requirements

### 7.1 Authentication
| Requirement | Implementation |
|-------------|---------------|
| Stateless auth | JWT (HS256, 256-bit secret via `JWT_SECRET` env var) |
| Access token TTL | 15 minutes (configurable via `JWT_ACCESS_TTL`) |
| Refresh token TTL | 7 days (configurable via `JWT_REFRESH_TTL`) |
| Token storage (client) | `localStorage` via `TokenStorageService` |
| Token blacklist | Redis (key = token, TTL = token expiry) |
| Password hashing | BCrypt, strength 10 (`BCRYPT_STRENGTH`) |

### 7.2 Authorization
- All protected endpoints validated server-side via Spring Security filter chain + `@PreAuthorize`.
- Frontend route guards (`AuthGuard`, `VendorGuard`, `AdminGuard`) are UI-only — server always enforces.
- Resource ownership: every service call derives user identity from JWT claims, never from client-supplied user IDs.

### 7.3 Input Validation
- All DTO fields annotated with Jakarta Validation (`@NotNull`, `@NotEmpty`, `@Email`, `@Min`, `@Max`, `@Size`, `@Pattern`).
- `GlobalExceptionHandler` maps `MethodArgumentNotValidException` → `400 Bad Request` with field-level error messages.
- Registration accepts any RFC-compliant email address (format validation only — no domain blocklist).
- New accounts start in `PENDING_VERIFICATION` status and are activated via OTP email verification (see §7.6).

### 7.4 Data Protection
- Soft delete on `User` and `Vendor` entities — records are never physically deleted, `deleted_at` timestamp set instead.
- All queries on `User` and `Vendor` filter `WHERE deleted_at IS NULL` by default.
- Hibernate Envers audit trail on `User`, `Booking`, `Vendor` tables.
- No secrets, API keys, or credentials hardcoded — all via environment variables.

### 7.5 Environment Variables (Required)
| Variable | Purpose |
|----------|---------|
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | 256-bit HS256 signing key |
| `GEMINI_API_KEY` | Google Gemini API key |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |
| `MAIL_HOST` | SMTP host for OTP verification emails |
| `MAIL_PORT` | SMTP port |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |
| `MAIL_FROM` | From-address for outbound OTP emails |

### 7.6 OTP-Based Email Verification
- New accounts start in `PENDING_VERIFICATION` status; login is rejected with `EMAIL_NOT_VERIFIED` (403) until verified.
- A 6-digit numeric OTP is generated on registration, stored in Redis keyed by email with a TTL (default 10 minutes), and emailed via SMTP (`MailService`).
- `POST /api/auth/verify-otp` validates the code and activates the account, issuing JWTs on success.
- Max 5 incorrect attempts before the code is invalidated and a resend is required; `POST /api/auth/resend-otp` is rate-limited by a cooldown (default 45 seconds).
- Seeded test accounts (`admin@test.com`, `vendor@test.com`, `user@test.com`) bypass this entirely — they are inserted directly with `ACTIVE` status.
- OTP values are never written to logs; `AuthenticationEventLogger` records only email/IP/user-agent/outcome for OTP events.

---

## 8. Data Model Summary

### Core Entity Relationships
```
User (1) ──────── (N) Booking
User (1) ──────── (1) Vendor
User (1) ──────── (N) ChatHistory
User (1) ──────── (N) RefreshToken

Vendor (1) ─────── (N) VendorService
Vendor (1) ─────── (N) VendorBooking
Vendor (1) ─────── (N) VendorDocument
Vendor (1) ─────── (N) PayoutRequest
Vendor (1) ─────── (N) WalletTransaction

Hotel (1) ──────── (N) Room
Hotel (1) ──────── (N) Booking
Room (1) ───────── (N) Booking

VendorService (1) ─ (N) ServiceAvailability
VendorService (1) ─ (N) VendorBooking
```

### Primary Key Strategy
| Entity Group | PK Type | Reason |
|-------------|---------|--------|
| User, Vendor, VendorService, WalletTransaction, PayoutRequest | UUID | Security (obfuscated IDs) |
| Hotel, Room, Booking | Long (auto-increment) | Legacy / high-volume sequential |

### Soft Delete Pattern
| Entity | Soft Delete Field |
|--------|------------------|
| `User` | `deleted_at` (Instant) |
| `Vendor` | `deleted_at` (Instant) *(planned)* |
| Others | Status enum-based deactivation |

---

## 9. API Design Principles

1. **RESTful conventions** — `GET` to read, `POST` to create, `PUT` to update, `DELETE` to remove.
2. **Consistent error responses** — All errors return `{ error, message, timestamp, path }`.
3. **Validation-first** — Every DTO validated before reaching service layer.
4. **Ownership enforcement** — Every service method verifies JWT-derived userId matches the requested resource.
5. **Pagination** — All list endpoints support `page` + `size` parameters.
6. **Versioning** — Vendor and admin APIs are under `/api/v1/`; public/legacy APIs under `/api/`.
7. **OpenAPI documentation** — All endpoints documented at `/swagger-ui.html`.

### Standard Error Response Shape
```json
{
  "error": "INVALID_CREDENTIALS",
  "message": "Human-readable description",
  "timestamp": "2026-07-12T10:00:00.000Z",
  "path": "/api/auth/login"
}
```

### HTTP Status Code Usage
| Status | Used For |
|--------|---------|
| 200 | Successful GET / update |
| 201 | Successful resource creation |
| 400 | Validation failure |
| 401 | Authentication failure / blocked domain |
| 403 | Insufficient role/permissions |
| 404 | Entity not found |
| 409 | Duplicate value (email, etc.) |
| 500 | Unhandled server error |

---

## 10. Non-Functional Requirements

### 10.1 Performance
| Requirement | Target |
|-------------|--------|
| API response time (p95) | < 300ms under normal load |
| Login endpoint | < 500ms (BCrypt hashing included) |
| Catalog search | < 200ms (with Redis cache) |
| Concurrent users (initial) | 100 simultaneous users |

### 10.2 Availability
- Target uptime: 99.5% (development/portfolio stage).
- Graceful shutdown: Spring Boot `server.shutdown=graceful`.

### 10.3 Security
- All traffic over HTTPS in production.
- CORS restricted to configured origins only.
- JWT secret minimum 256 bits; rotatable without downtime via Redis blacklist.
- Rate limiting on auth endpoints *(planned — not yet implemented)*.

### 10.4 Scalability
- Stateless backend (JWT) supports horizontal scaling.
- Redis shared cache/token store works across multiple backend instances.
- MySQL connection pool (HikariCP, defaults).

### 10.5 Observability
- Structured JSON logs (Logstash-compatible) for all business and security events.
- `SecurityEventLogger` records: login success/failure, registration, token refresh, logout.
- Prometheus metrics exposed at `/actuator/prometheus`.
- Spring Actuator health endpoint at `/actuator/health`.

### 10.6 Maintainability
- Consistent package structure: `api/`, `services/`, `entities/`, `repositories/`, `dto/`, `enums/`, `config/`.
- DTOs used for all request/response — entities never exposed directly.
- Global exception handler centralizes all error responses.
- Constants for magic strings (`EntityConstants`, `SecurityConstants`, `ValidationConstants`).

---

## 11. Role & Permission Matrix

| Feature | `USER` | `VENDOR` | `ADMIN` | `SUPER_ADMIN` |
|---------|--------|----------|---------|---------------|
| Register / Login | ✅ | ✅ | ✅ | ✅ |
| Browse service catalog | ✅ | ✅ | ✅ | ✅ |
| Create hotel booking | ✅ | ✅ | ✅ | ✅ |
| View own bookings | ✅ | ✅ | ✅ | ✅ |
| AI chat | ✅ | ✅ | ✅ | ✅ |
| Register as vendor | ✅ | — | — | — |
| Manage own services | — | ✅ | — | — |
| Confirm/reject bookings (vendor) | — | ✅ | — | — |
| View vendor wallet | — | ✅ | — | — |
| Request payout | — | ✅ | — | — |
| View vendor analytics | — | ✅ | — | — |
| Approve/reject vendors | — | — | ✅ | ✅ |
| Suspend vendors | — | — | ✅ | ✅ |
| Manage users | — | — | ✅ | ✅ |
| Manage banners | — | — | ✅ | ✅ |
| System settings | — | — | — | ✅ |
| Commission configuration | — | — | — | ✅ |

---

## 12. Current Status & Completion

### ✅ Completed Features

**Authentication & Security**
- [x] JWT registration, login, refresh, logout
- [x] BCrypt password hashing
- [x] Account lockout (5 attempts, 15 min)
- [x] Token blacklisting via Redis
- [x] Soft delete for users
- [x] Role-based route guards (Angular)
- [x] Security event logging
- [x] Email domain blocklist (hotmail.com, email.com, test.com blocked)

**Vendor Portal**
- [x] Vendor registration & document upload
- [x] Admin approval workflow (Pending → Approved/Rejected/Suspended)
- [x] Vendor service CRUD (with availability, pricing, booking mode)
- [x] Vendor booking inbox (confirm/reject)
- [x] Wallet & transaction history
- [x] Payout request management
- [x] Revenue analytics & KPI dashboard
- [x] Booking trend charts

**Admin Panel**
- [x] Vendor approval queue
- [x] User management
- [x] Banner CRUD
- [x] System-wide booking analytics

**Booking System**
- [x] Hotel & room listing
- [x] Room availability check
- [x] Booking creation and lifecycle
- [x] Booking auto-completion scheduler
- [x] Vendor wallet credit on completion

**Travel Content**
- [x] Tourist spots, destinations, transport, foods, items, markets, packages

**AI Features**
- [x] Gemini AI chat integration
- [x] Chat history persistence

**Infrastructure**
- [x] Docker + Docker Compose configuration
- [x] Prometheus metrics
- [x] Spring Actuator health checks
- [x] OpenAPI / Swagger UI
- [x] Redis caching

---

## 13. Known Gaps & Future Work

### 🔴 High Priority (Production Blockers)
| # | Gap | Notes |
|---|-----|-------|
| G1 | **Rate limiting on auth endpoints** | `/api/auth/login` and `/register` have no rate limiting — vulnerable to brute force beyond account lockout |
| G2 | **File upload security** | Uploaded files (vendor documents, banners, service images) have no virus scanning or file-type whitelisting |
| G3 | **Password reset flow** | No email-based password reset implemented yet |
| G4 | **HTTPS enforcement** | Needs SSL termination config for production deployment |
| G5 | **Refresh token rotation / reuse detection** | Refresh tokens are rotated on use, but stolen token reuse detection (refresh token family) is not yet implemented |

### 🟡 Medium Priority (Feature Completeness)
| # | Gap | Notes |
|---|-----|-------|
| G6 | **Email notifications** | No transactional emails on booking confirmation, payout approval, or vendor approval |
| G7 | **AI context injection** | AI chat is stateless per message; no awareness of user's existing bookings or preferences |
| G8 | **Search & filtering** | Catalog search exists but needs full-text search (consider MySQL FULLTEXT or Elasticsearch) |
| G9 | **Payment gateway integration** | Payouts use enum methods but no real payment processor (Stripe, PayPal) integrated |
| G10 | **Super Admin features** | Role exists but commission config and system settings UI not fully built |
| G11 | **User booking cancellation** | Endpoint exists but cancellation policy (refund logic, timeline restrictions) not enforced |

### 🟢 Nice-to-Have (Portfolio Polish)
| # | Gap | Notes |
|---|-----|-------|
| G12 | **Real-time notifications** | WebSocket or SSE for live booking/approval status updates |
| G13 | **Reviews & ratings** | Users rating vendor services post-booking |
| G14 | **Multi-language support (i18n)** | Especially relevant for Southeast Asian market |
| G15 | **Image CDN** | Currently serving uploads from disk; should use S3/CloudFront in production |
| G16 | **Mobile-responsive polish** | Angular SSR enabled but mobile UX not fully validated |
| G17 | **Audit log viewer** | Hibernate Envers is configured but no admin UI to browse audit history |

---

*This PRD was generated from live codebase analysis on July 12, 2026.*  
*Last updated: July 12, 2026*


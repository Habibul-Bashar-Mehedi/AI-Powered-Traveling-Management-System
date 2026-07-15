# OTP (One-Time Password) System

## Purpose

Email verification during **user registration only**. No password-reset OTP flow exists (planned as `FR-AUTH-006`).

---

## Architecture Overview

```
┌──────────────┐       ┌───────────────────┐       ┌──────────────┐
│  Angular FE  │ ───→  │  AuthController   │ ───→  │ AuthService  │
│  (verify-otp │       │  POST /api/auth/* │       │ (impl)      │
│   component) │ ←───  │                   │ ←───  │              │
└──────────────┘       └───────────────────┘       └──────┬───────┘
                                                          │
                      ┌───────────────────────────────────┤
                      │                                   │
               ┌──────┴──────┐                    ┌───────┴───────┐
               │ OtpService  │                    │ MailService   │
               │ (MySQL)     │                    │ (SMTP/Gmail)  │
               └─────────────┘                    └───────────────┘
```

**Key principle:** OTP state is stored in the `otp_verifications` MySQL table (one row per email). No Redis usage for OTP.

---

## Database Table: `otp_verifications`

| Column | Type | Notes |
|--------|------|-------|
| `id` | `BIGINT AUTO_INCREMENT` | Primary key |
| `version` | `INT` | Optimistic locking |
| `email` | `VARCHAR(100) UNIQUE` | One row per email |
| `otp_code` | `VARCHAR(6)` | 6-digit code |
| `attempts` | `INT` | Failed verification count |
| `expires_at` | `DATETIME` | Code expiry (10 min from creation) |
| `last_resend_at` | `DATETIME NULL` | Tracks resend cooldown |
| `verified_at` | `DATETIME NULL` | Set to now on success |
| `created_at` | `DATETIME` | Row creation timestamp |
| `updated_at` | `DATETIME` | Row update timestamp |

---

## Endpoints

| Method | Path | DTO | Purpose |
|--------|------|-----|---------|
| `POST` | `/api/auth/verify-otp` | `{ email, otp }` | Verify 6-digit code |
| `POST` | `/api/auth/resend-otp` | `{ email }` | Request new code (cooldown: 45s) |

---

## Configuration (`application.properties`)

| Property | Env Variable | Default | Description |
|----------|-------------|---------|-------------|
| `app.otp.ttl-minutes` | `OTP_TTL_MINUTES` | `10` | Code validity duration |
| `app.otp.max-attempts` | `OTP_MAX_ATTEMPTS` | `5` | Max failed attempts before code invalidated |
| `app.otp.resend-cooldown-seconds` | `OTP_RESEND_COOLDOWN_SECONDS` | `45` | Min wait between resends |

---

## Full Flow

### 1. Registration → OTP Generation

1. **Frontend** → `POST /api/auth/register` with `{ username, email, password, role, countryId }`
2. **`AuthenticationServiceImpl.register()`** creates `User` with `status = PENDING_VERIFICATION`, saves to MySQL
3. Calls `OtpServiceImpl.generateAndSend(email)`:
   - Deletes any existing OTP row for this email
   - Creates new `OtpVerification` row with: 6-digit code, `attempts = 0`, `expires_at = now + 10min`
   - Sends email via SMTP (Gmail): *"Your verification code is: {code}"*
4. Returns `RegisterResponse` (no tokens) — user must verify first
5. **Frontend** navigates to `/verify-otp?email=...`

### 2. OTP Verification

1. **Frontend** → `POST /api/auth/verify-otp` with `{ email, otp }`
2. **`OtpServiceImpl.verify()`** reads `OtpVerification` row by email from MySQL:
   - **Not found** → `OTP_EXPIRED` (400)
   - **Already verified or expired** → `OTP_EXPIRED` (400)
   - **Code mismatch** → increment `attempts`:
     - Attempts ≥ max (5) → delete the row → `OTP_MAX_ATTEMPTS` (400)
     - Otherwise → `OTP_INVALID` (400)
   - **Code matches** → set `verified_at = now`, save row
3. On success, `AuthenticationServiceImpl` sets `user.status = ACTIVE`, saves to DB, generates **JWT access + refresh tokens**, returns `AuthResponse`

### 3. OTP Resend

1. **Frontend** → `POST /api/auth/resend-otp` with `{ email }`
2. **`OtpServiceImpl.canResend()`** checks `last_resend_at` on the row:
   - If `null` or `last_resend_at + 45s < now` → can resend
   - Otherwise → `OTP_RESEND_COOLDOWN` (429)
3. If user exists with `PENDING_VERIFICATION`:
   - Calls `generateAndSend()` (replaces old row)
   - Calls `markResent()` (sets `last_resend_at = now`)
4. If user not found (or already active) → returns 200 anyway — **enumeration protection**

### 4. Login with Unverified Account

Same as before — `AuthenticationServiceImpl.login()` checks `user.getStatus()`.

---

## Entity: `OtpVerification`

**File:** `aptms/entities/OtpVerification.java`

```java
@Entity
@Audited
@Data
@Table(name = "otp_verifications")
public class OtpVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_resend_at")
    private Instant lastResendAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;
    // ... timestamps + isExpired() / isVerified() helpers
}
```

---

## Error Codes

| Code | HTTP | When |
|------|------|------|
| `OTP_EXPIRED` | 400 | Code expired, already verified, or never sent |
| `OTP_INVALID` | 400 | Wrong code |
| `OTP_MAX_ATTEMPTS` | 400 | Exceeded 5 failed attempts |
| `OTP_RESEND_COOLDOWN` | 429 | Resend before 45s cooldown |
| `EMAIL_NOT_VERIFIED` | 403 | Login with unverified account |

---

## Key Files

| File | Role |
|------|------|
| `aptms/entities/OtpVerification.java` | JPA entity |
| `aptms/repositories/OtpVerificationRepository.java` | JPA repository |
| `aptms/services/OtpService.java` | Interface |
| `aptms/services/impl/OtpServiceImpl.java` | DB-backed implementation |
| `aptms/services/MailService.java` | Mail interface |
| `aptms/services/impl/MailServiceImpl.java` | SMTP email sending |
| `aptms/api/AuthController.java` | REST endpoints |
| `aptms/exceptions/OtpException.java` | Custom exception |
| `aptms/exceptions/EmailNotVerifiedException.java` | Login guard |
| `aptms/enums/UserStatus.java` | `PENDING_VERIFICATION`, `ACTIVE` |
| `resources/db/migration/V010__create_otp_verifications_table.sql` | Flyway migration |

---

## Design Decisions & Notes

- **MySQL-backed** — OTP stored in `otp_verifications` table, not Redis. One row per email (unique constraint).
- **No OTP in logs** — Event logger explicitly excludes the code value.
- **SMTP failure tolerant** — If sending email fails, the OTP row is still saved; user can resend.
- **Enumeration protection** — `/resend-otp` returns 200 even for unknown emails.
- **No password reset OTP** — Not implemented yet (planned).
- **Cooldown mismatch risk** — Frontend hardcodes 45s; backend is configurable via env var.
- **Stale row cleanup** — No automated cleanup of expired/unverified rows. They remain in the table but are treated as expired by the `isExpired()` check. A scheduled cleanup could be added later.

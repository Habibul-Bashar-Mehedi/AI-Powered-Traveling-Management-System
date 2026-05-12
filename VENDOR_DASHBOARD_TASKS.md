# Vendor Dashboard — Implementation Task Tracker
## AITMS-BRD-VND-001 · v1.0.0

**Started:** 2026-05-11  
**Stack:** Java 21 + Spring Boot 4 (Backend) · Angular 17+ (Frontend)

---

## Progress Legend
| Symbol | Status |
|--------|--------|
| ✅ | Done |
| 🔄 | In Progress |
| ⏳ | Pending |
| ❌ | Blocked |

---

## 🗂️ BACKEND TASKS

### Phase 1 — Enums
| # | Task | File | Status |
|---|------|------|--------|
| BE-01 | VendorType enum (HOTEL, TOUR_GUIDE, TRANSPORT) | `enums/VendorType.java` | ✅ |
| BE-02 | VendorStatus enum (PENDING_REVIEW, APPROVED, REJECTED, SUSPENDED) | `enums/VendorStatus.java` | ✅ |
| BE-03 | ServiceType enum (HOTEL_ROOM, TOUR_PACKAGE, TRANSPORT_ROUTE) | `enums/ServiceType.java` | ✅ |
| BE-04 | ServiceStatus enum (DRAFT, ACTIVE, INACTIVE) | `enums/ServiceStatus.java` | ✅ |
| BE-05 | PricingUnit enum (PER_NIGHT, PER_PERSON, PER_SEAT, PER_TRIP) | `enums/PricingUnit.java` | ✅ |
| BE-06 | BookingMode enum (INSTANT, MANUAL) | `enums/BookingMode.java` | ✅ |
| BE-07 | DocumentType enum | `enums/DocumentType.java` | ✅ |
| BE-08 | VendorBookingStatus enum | `enums/VendorBookingStatus.java` | ✅ |
| BE-09 | PaymentStatus enum | `enums/PaymentStatus.java` | ✅ |
| BE-10 | TransactionType enum | `enums/TransactionType.java` | ✅ |
| BE-11 | PayoutStatus enum | `enums/PayoutStatus.java` | ✅ |
| BE-12 | CancelledBy enum | `enums/CancelledBy.java` | ✅ |
| BE-13 | PayoutMethod enum | `enums/PayoutMethod.java` | ✅ |

### Phase 2 — Entities
| # | Task | File | Status |
|---|------|------|--------|
| BE-14 | Vendor entity (all fields per BRD §6.1) | `entities/Vendor.java` | ✅ |
| BE-15 | VendorDocument entity (§6.4) | `entities/VendorDocument.java` | ✅ |
| BE-16 | VendorService entity (§6.2) | `entities/VendorService.java` | ✅ |
| BE-17 | ServiceAvailability entity | `entities/ServiceAvailability.java` | ✅ |
| BE-18 | VendorBooking entity (§6.3) | `entities/VendorBooking.java` | ✅ |
| BE-19 | WalletTransaction entity | `entities/WalletTransaction.java` | ✅ |
| BE-20 | PayoutRequest entity | `entities/PayoutRequest.java` | ✅ |

### Phase 3 — Repositories
| # | Task | File | Status |
|---|------|------|--------|
| BE-21 | VendorRepository | `repositories/VendorRepository.java` | ✅ |
| BE-22 | VendorDocumentRepository | `repositories/VendorDocumentRepository.java` | ✅ |
| BE-23 | VendorServiceRepository | `repositories/VendorServiceRepository.java` | ✅ |
| BE-24 | ServiceAvailabilityRepository | `repositories/ServiceAvailabilityRepository.java` | ✅ |
| BE-25 | VendorBookingRepository | `repositories/VendorBookingRepository.java` | ✅ |
| BE-26 | WalletTransactionRepository | `repositories/WalletTransactionRepository.java` | ✅ |
| BE-27 | PayoutRequestRepository | `repositories/PayoutRequestRepository.java` | ✅ |

### Phase 4 — DTOs
| # | Task | File | Status |
|---|------|------|--------|
| BE-28 | VendorRegistrationRequest DTO | `dto/vendor/VendorRegistrationRequest.java` | ✅ |
| BE-29 | VendorProfileDTO | `dto/vendor/VendorProfileDTO.java` | ✅ |
| BE-30 | VendorServiceRequest / Response DTO | `dto/vendor/VendorServiceDTO.java` | ✅ |
| BE-31 | VendorBookingDTO | `dto/vendor/VendorBookingDTO.java` | ✅ |
| BE-32 | WalletSummaryDTO | `dto/vendor/WalletSummaryDTO.java` | ✅ |
| BE-33 | PayoutRequestDTO | `dto/vendor/PayoutRequestDTO.java` | ✅ |
| BE-34 | AnalyticsSummaryDTO | `dto/vendor/AnalyticsSummaryDTO.java` | ✅ |
| BE-35 | AdminVendorActionDTO | `dto/vendor/AdminVendorActionDTO.java` | ✅ |

### Phase 5 — Services
| # | Task | File | Status |
|---|------|------|--------|
| BE-36 | VendorRegistrationService interface + impl | `services/VendorRegistrationService.java` + impl | ✅ |
| BE-37 | VendorProfileService interface + impl | `services/VendorProfileService.java` + impl | ✅ |
| BE-38 | VendorServiceMgmtService interface + impl | `services/VendorServiceMgmtService.java` + impl | ✅ |
| BE-39 | VendorBookingService interface + impl | `services/VendorBookingService.java` + impl | ✅ |
| BE-40 | VendorWalletService interface + impl | `services/VendorWalletService.java` + impl | ✅ |
| BE-41 | VendorAnalyticsService interface + impl | `services/VendorAnalyticsService.java` + impl | ✅ |
| BE-42 | AdminVendorService interface + impl | `services/AdminVendorService.java` + impl | ✅ |

### Phase 6 — Controllers
| # | Task | File | Status |
|---|------|------|--------|
| BE-43 | VendorRegistrationController (PUBLIC) | `api/VendorRegistrationController.java` | ✅ |
| BE-44 | VendorProfileController (VENDOR) | `api/VendorProfileController.java` | ✅ |
| BE-45 | VendorServiceController (VENDOR) | `api/VendorServiceController.java` | ✅ |
| BE-46 | VendorBookingController (VENDOR) | `api/VendorBookingController.java` | ✅ |
| BE-47 | VendorWalletController (VENDOR) | `api/VendorWalletController.java` | ✅ |
| BE-48 | VendorAnalyticsController (VENDOR) | `api/VendorAnalyticsController.java` | ✅ |
| BE-49 | AdminVendorController (ADMIN) | `api/AdminVendorController.java` | ✅ |

### Phase 7 — Security & Config
| # | Task | File | Status |
|---|------|------|--------|
| BE-50 | Update SecurityConfig for vendor endpoints | `security/SecurityConfig.java` | ✅ |
| BE-51 | Fix vendor register endpoint: require `authenticated()` instead of `permitAll()` | `security/SecurityConfig.java` | ✅ |

---

## 🔧 POST-INITIAL IMPROVEMENTS (Session 2)

### Backend Fixes & Enhancements
| # | Task | File | Status |
|---|------|------|--------|
| BE-52 | Fix `VendorAnalyticsServiceImpl` — period-specific revenue (`revenueToday/Week/Month`) | `services/impl/VendorAnalyticsServiceImpl.java` | ✅ |
| BE-53 | Add `IllegalStateException` handler to `GlobalExceptionHandler` (409 Conflict) | `exceptions/GlobalExceptionHandler.java` | ✅ |
| BE-54 | Create `BookingCompletionScheduler` — auto-complete bookings & settle wallet (BRD FR-WAL-007) | `services/BookingCompletionScheduler.java` | ✅ |
| BE-55 | Add Flyway SQL migration for all vendor tables (V005) | `db/migration/V005__create_vendor_tables.sql` | ✅ |

### Frontend Enhancements
| # | Task | File | Status |
|---|------|------|--------|
| FE-20 | Enhance analytics KPI grid — add Today/Week/Month revenue cards | `vendor/vendor-analytics/vendor-analytics.html` | ✅ |
| FE-21 | Add KPI colour-accent CSS classes for analytics cards | `vendor/vendor-analytics/vendor-analytics.css` | ✅ |

---

## 🖥️ FRONTEND TASKS

### Phase 1 — Models & Enums
| # | Task | File | Status |
|---|------|------|--------|
| FE-01 | vendor.model.ts (all interfaces) | `models/vendor.model.ts` | ✅ |
| FE-02 | vendor.enums.ts | `enums/vendor.enums.ts` | ✅ |
| FE-03 | Update api-endpoints.ts with vendor endpoints | `constants/api-endpoints.ts` | ✅ |

### Phase 2 — Services
| # | Task | File | Status |
|---|------|------|--------|
| FE-04 | vendor.service.ts (registration, profile) | `services/vendor.service.ts` | ✅ |
| FE-05 | vendor-booking.service.ts | `services/vendor-booking.service.ts` | ✅ |
| FE-06 | vendor-wallet.service.ts | `services/vendor-wallet.service.ts` | ✅ |
| FE-07 | vendor-analytics.service.ts | `services/vendor-analytics.service.ts` | ✅ |

### Phase 3 — Guards
| # | Task | File | Status |
|---|------|------|--------|
| FE-08 | vendor.guard.ts (VENDOR role guard) | `guards/vendor.guard.ts` | ✅ |
| FE-09 | admin.guard.ts (ADMIN role guard) | `guards/admin.guard.ts` | ✅ |

### Phase 4 — Vendor Components
| # | Task | File | Status |
|---|------|------|--------|
| FE-10 | vendor-registration (multi-step form) | `vendor/vendor-registration/` | ✅ |
| FE-11 | vendor-dashboard (main layout) | `vendor/vendor-dashboard/` | ✅ |
| FE-12 | vendor-overview (stats overview) | `vendor/vendor-overview/` | ✅ |
| FE-13 | vendor-services (service CRUD) | `vendor/vendor-services/` | ✅ |
| FE-14 | vendor-bookings (booking inbox) | `vendor/vendor-bookings/` | ✅ |
| FE-15 | vendor-wallet (earnings & payout) | `vendor/vendor-wallet/` | ✅ |
| FE-16 | vendor-analytics (charts & reports) | `vendor/vendor-analytics/` | ✅ |

### Phase 5 — Admin Components
| # | Task | File | Status |
|---|------|------|--------|
| FE-17 | admin-vendor-management (approve/reject/suspend) | `admin/vendor-management/` | ✅ |

### Phase 6 — Routing & Integration
| # | Task | File | Status |
|---|------|------|--------|
| FE-18 | Update app.routes.ts with vendor/admin routes | `app.routes.ts` | ✅ |
| FE-19 | Update login redirect by role | `login/login.ts` | ✅ |

---

## 🗺️ API Endpoint Map

| Method | Endpoint | Role | Task Ref |
|--------|----------|------|----------|
| POST | `/api/v1/vendor/register` | PUBLIC | BE-43 |
| GET | `/api/v1/vendor/profile` | VENDOR | BE-44 |
| PUT | `/api/v1/vendor/profile` | VENDOR | BE-44 |
| GET | `/api/v1/vendor/services` | VENDOR | BE-45 |
| POST | `/api/v1/vendor/services` | VENDOR | BE-45 |
| PUT | `/api/v1/vendor/services/{id}` | VENDOR | BE-45 |
| DELETE | `/api/v1/vendor/services/{id}` | VENDOR | BE-45 |
| GET | `/api/v1/vendor/bookings` | VENDOR | BE-46 |
| POST | `/api/v1/vendor/bookings/{id}/confirm` | VENDOR | BE-46 |
| POST | `/api/v1/vendor/bookings/{id}/reject` | VENDOR | BE-46 |
| GET | `/api/v1/vendor/wallet` | VENDOR | BE-47 |
| POST | `/api/v1/vendor/wallet/payout` | VENDOR | BE-47 |
| GET | `/api/v1/vendor/analytics/summary` | VENDOR | BE-48 |
| GET | `/api/v1/admin/vendors/pending` | ADMIN | BE-49 |
| POST | `/api/v1/admin/vendors/{id}/approve` | ADMIN | BE-49 |
| POST | `/api/v1/admin/vendors/{id}/reject` | ADMIN | BE-49 |
| POST | `/api/v1/admin/vendors/{id}/suspend` | ADMIN | BE-49 |

---

## 📊 Overall Progress

| Phase | Total | Done | Remaining |
|-------|-------|------|-----------|
| Backend Enums | 13 | 13 | 0 |
| Backend Entities | 7 | 7 | 0 |
| Backend Repositories | 7 | 7 | 0 |
| Backend DTOs | 8 | 8 | 0 |
| Backend Services | 7 | 7 | 0 |
| Backend Controllers | 7 | 7 | 0 |
| Backend Config | 2 | 2 | 0 |
| Backend Fixes (Session 2) | 4 | 4 | 0 |
| Frontend Models | 3 | 3 | 0 |
| Frontend Services | 4 | 4 | 0 |
| Frontend Guards | 2 | 2 | 0 |
| Frontend Components | 8 | 8 | 0 |
| Frontend Routing | 2 | 2 | 0 |
| Frontend Enhancements (Session 2) | 2 | 2 | 0 |
| **TOTAL** | **76** | **76** | **0** |

---

*Last updated: 2026-05-11 — Session 2 improvements applied*


# APTMS — Project Activity Gantt Chart

Development timeline for the AI-Powered Traveling Management System (solo developer, academic/portfolio project). Dates are illustrative (relative durations, not calendar commitments) — adjust the `dateFormat` start date and durations to match your actual timeline.

```mermaid
gantt
    title APTMS Development Timeline
    dateFormat  YYYY-MM-DD
    axisFormat  %b %d
    excludes    weekends

    section Phase 1: Core Platform (Completed)
    Authentication & JWT Security           :done, auth, 2026-01-05, 10d
    User/Vendor/Admin Roles                 :done, roles, after auth, 5d
    Hotel Booking System                    :done, hotelbook, after roles, 8d
    Vendor Portal (reg/docs/wallet)         :done, vendorportal, after roles, 12d
    Admin Dashboard                         :done, admindash, after vendorportal, 6d
    Travel Content Modules (7 types)        :done, content, after admindash, 10d
    AI Chat Assistant (Gemini)              :done, aichat, after content, 5d
    Docker Containerization                 :done, docker, after aichat, 4d

    section Phase 2: Feature Enhancement
    Email Validation Fix + OTP Verification :emailfix, after docker, 4d
    Vendor Suspension & Reinstatement Flow   :suspend, after emailfix, 3d
    File Upload Security                     :uploadsec, after emailfix, 4d
    Unified Booking History                  :unifiedbook, after emailfix, 8d
    Dashboard Card Redesign + Details Modal  :dashredesign, after unifiedbook, 5d
    Custom Package Bundling                  :packagebundle, after dashredesign, 7d
    Traditional Food/Places Discovery        :fooddiscovery, after packagebundle, 6d
    Expense Tracking by Destination          :expense, after unifiedbook, 5d
    Payment Gateway Integration (SSLCommerz) :payment, after unifiedbook, 6d

    section Phase 3: Hardening & Testing
    AI Chat Context-Awareness Enhancement    :aicontext, after expense, 5d
    API Performance Optimization             :perf, after payment, 5d
    Automated Integration Test Suite         :inttest, after perf, 8d
```

## Notes on dependencies

- **Email Validation Fix + OTP** is scheduled first in Phase 2 since auth stability underlies most later features.
- **Unified Booking History** gates **Expense Tracking**, **Payment Gateway Integration**, and (indirectly, via context data) **AI Chat Context-Awareness** — none of these can be built correctly against two divergent booking models.
- **Dashboard Redesign** depends on Unified Booking History since the "view details" modal needs a single booking shape to render against.
- **Automated Integration Test Suite** is scheduled last, after the feature surface (performance work included) stabilizes, so tests aren't rewritten mid-flight.
- No task is marked `active` — update the relevant task's status tag (e.g. `active, emailfix, ...`) once you confirm which Phase 2 item you're currently on.

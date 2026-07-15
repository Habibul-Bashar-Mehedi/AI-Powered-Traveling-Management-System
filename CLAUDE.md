Before making any change to this project (SMTS — Smart Travel Management System: Spring Boot backend with JPA/Hibernate, MySQL, Spring Security/JWT, Redis; Angular frontend with Tailwind CSS 4.1), always follow this process:

1. SCAN BEFORE CHANGING
    - Before writing any new code, search the existing codebase for related entities, services, controllers, components, or config that already touch this area
    - Never assume something doesn't exist — verify by searching first
    - If a similar pattern already exists elsewhere in the project (e.g. a booking flow, a card component, a modal, a validation approach), reuse and extend that pattern rather than inventing a new one, so the codebase stays consistent
    - If you find existing code that conflicts with what I'm asking for, tell me before proceeding — don't silently overwrite or duplicate logic
2. REPORT FINDINGS FIRST ON NON-TRIVIAL CHANGES
    - For anything beyond a small isolated fix, show me what you found (relevant files, current structure, gaps) and your intended approach before writing the full implementation
    - Flag any design decisions that have real tradeoffs (e.g. data model choices, whether to store something in DB vs. disk, security implications) rather than silently picking one
3. KEEP THE PROJECT CONSISTENT
    - Match existing naming conventions, package structure, DTO patterns, error-handling style, and security enforcement already used elsewhere
    - Reuse shared/reusable components (cards, modals, forms) instead of creating near-duplicates
    - If you notice inconsistency already in the codebase (e.g. two different booking patterns, inconsistent naming, mixed validation approaches), point it out to me — don't just match the nearest example blindly if it's clearly a past mistake
4. FLAG WHAT'S LEFT UNFINISHED OR RISKY
    - After implementing a feature, tell me clearly: what's done, what's stubbed/TODO, what depends on something not yet built, and what should be revisited later
    - Never silently leave placeholder/mock logic in place without telling me it's a placeholder
5. PROACTIVELY SUGGEST IMPROVEMENTS (when relevant, not constantly)
    - If you notice something that would make the project look more professional or production-ready, mention it briefly at the end of your response as a suggestion
    - Prioritize suggestions that matter for a portfolio/interview context: clean architecture, security correctness, and polish
6. SECURITY BASELINE (always enforce, don't ask)
    - Never hardcode API keys, passwords, or secrets — always environment variables
    - Always verify resource ownership via JWT-derived user ID, never trust a client-supplied user ID
    - Enforce role-based access server-side, not just by hiding UI elements
    - Validate and sanitize all user input
7. WHEN IN DOUBT, ASK — DON'T GUESS ON SCOPE
    - If a request could reasonably be implemented multiple ways with meaningfully different scope or complexity, briefly describe the options and ask which I want, rather than picking the most complex one by default

Apply this process to every task in this project unless I explicitly tell you to skip investigation for a quick, isolated fix.

---

## PROJECT FACTS (current as of PRD v1.0, July 12, 2026 — verify against code if anything seems stale)

**Two separate booking systems exist — do not assume they're unified:**
- `Booking` (Hotel/Room, Long auto-increment PK) — direct hotel booking
- `VendorBooking` (generic vendor services: HOTEL_ROOM/TOUR_PACKAGE/TRANSPORT_ROUTE via `VendorService`, likely UUID PK)
There is no single "all bookings" table. Any feature needing full booking history must merge both.

**Already implemented — do not rebuild:**
- AI chat: `POST /api/ai/chat` + `GET /api/ai/chat/history` (Gemini-backed, persisted history) — stateless per message, context injection is a known gap, not a missing feature
- Travel content CRUD: Tourist Spots, Destinations, Transport, Traditional Foods, Traditional Items, Markets, Travel Packages (`/api/tourist-spot`, `/api/destination`, `/api/transport`, `/api/food`, `/api/items`, `/api/market`, `/api/package`)
- Image uploads: disk-based storage in `/uploads/`, file path stored in DB — this is the correct, intentional pattern, not a bug
- Vendor portal: registration, document upload, admin approval workflow, service CRUD, booking inbox, wallet/payouts, analytics
- Admin panel: vendor approval queue, user management, banner CRUD, booking analytics

**No `Location` entity exists — and none should be created.** Use the existing `Destination` entity for any location-grouping features.

**Known real bugs / inconsistencies:**
- Backend registration blocklists `hotmail.com`, `email.com`, `test.com` (documented in security requirements as intentional, but contradicts the fact that `*@test.com` test accounts are seeded directly into the DB, bypassing registration)
- Frontend separately blocks `@gmail.com` via a `gmailNotAllowed` validator, independent of the backend blocklist
- Both restrictions need removing in favor of standard email format validation only

**Confirmed gaps (from PRD Section 13) — real, unaddressed:**
- No rate limiting on `/api/auth/login` or `/register`
- No file upload security (type whitelist / virus scanning) on vendor docs, banners, service images
- No password reset flow (`FR-AUTH-006` marked "Planned")
- No OTP-based email verification on registration
- Refresh tokens rotate on use, but stolen-token-reuse detection is not implemented
- No real payment gateway integrated (payouts use enum methods only) — SSLCommerz chosen as the integration target
- Booking cancellation endpoint exists but refund/timeline policy is not enforced
- AI chat has no awareness of user's bookings/preferences yet

**Stack correction:** Frontend is Angular 21 + Tailwind CSS 4.1. There is no Bootstrap in this stack — ignore any earlier prompt language mentioning Bootstrap.

When a task touches any of the above, treat this section as ground truth over re-investigation — but still verify against actual code if something seems inconsistent, since this file can go stale as the project evolves.

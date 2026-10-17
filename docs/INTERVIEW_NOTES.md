# AgroConnect - design notes for a project review / Java interview

## One-paragraph pitch
AgroConnect lets fruit farmers find suitable bulk buyers (and merchants find farmers) ranked by fruit, location, price and
reputation, then keeps an honest, two-party-confirmed record of meetings, deals, deliveries and payments. It is a
record-keeping and discovery platform, not a payment gateway.

## Architecture
Browser (HTML/CSS/vanilla JS + fetch) -> Spring Boot REST controllers (thin) -> services (all business rules) ->
Spring Data JPA repositories -> MySQL. Spring Security guards every URL by role; services re-check ownership.
Static pages are served by the same Spring Boot app, so there is one thing to run and no CORS to explain.

## Decisions and why
* **One `users` table with a role** - farmers, merchants and admin share login and profile logic; role-specific columns are nullable.
* **Session cookie + BCrypt, not JWT** - simpler, works naturally with `fetch`, fewer moving parts to explain. CSRF is off (documented limitation).
* **Kilograms everywhere, `BigDecimal` for money** - tons are converted only at the edge; no floating-point errors in totals.
* **Totals are never accepted from the browser** - deal value, remaining kg and outstanding rupees are recomputed on the server.
* **Row lock on the deal (`PESSIMISTIC_WRITE`)** - two people submitting at once cannot push deliveries or payments past the limits.
* **Ratings and counts are derived** from stored reviews/deals each time - no hand-typed trust numbers.
* **Phone privacy in one class** - `ContactAccessService` is the only place that decides who sees a number; tested separately.
* **Two-party approvals** - a deal becomes active only when the *other* person accepts; completion is a request the other person accepts. Stored in `deal_approvals` with who/when/what.
* **Payments are never "Verified"** - a user-entered record is not proof of money moving.
* **Audit log** - sensitive admin actions are written to `audit_logs`; admins cannot edit deals, supplies or payments.
* **`StorageService` interface** - local disk today, cloud storage later without touching callers.

## Likely questions
* *How do you stop a farmer reading another farmer's deals?* Role rules in `SecurityConfig` plus `DealService.locked()` which checks the caller is one of the two participants (403 otherwise).
* *Where is the phone-number rule enforced?* Backend: `ContactAccessService`; the JSON simply contains no phone unless allowed.
* *What if two requests arrive at once?* The deal row is locked inside the transaction, so the second request sees the first one's totals.
* *Why not store the deal total?* It is stored, but only ever calculated by the service (`quantity x price`); the browser has no field for it.
* *What would you do next?* CSRF tokens, password reset, a real payment-verification flow, notifications, map-based distance, integration tests with Testcontainers.

## Honest limits (repeat these yourself in a review)
37 unit tests, no integration/browser tests; CSRF off; no real payment verification; local file storage; small district-neighbour list.

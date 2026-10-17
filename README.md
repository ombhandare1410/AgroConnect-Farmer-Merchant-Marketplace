# AgroConnect - "Better Buyers. Brighter Futures."

A farmer-first online marketplace that connects fruit farmers directly with bulk fruit merchants.
Farmers list fruit; merchants post what they want to buy; the platform ranks matches by fruit and location,
and keeps transparent, two-party-confirmed records of meetings, deals, deliveries and payments.
English, Marathi and Hindi. Final-year project (Computer Engineering).

**Stack:** HTML5 / CSS3 / vanilla JavaScript - Java 17 + Spring Boot 3.5 (Spring Security, Spring Data JPA) - MySQL 8 - Maven.

## Run it
Requirements: JDK 17, Maven 3.9+, MySQL 8 running locally, an IDE with the Lombok plugin.

1. Set environment variables (see `.env.example`).
   * Windows PowerShell: `setx DB_PASSWORD "your_password"`, `setx ADMIN_EMAIL "admin@example.com"`, `setx ADMIN_PASSWORD "a_strong_password"` - then **open a new terminal**.
   * Mac/Linux: `export DB_PASSWORD="your_password"` (and the others).
2. From the project folder: `mvn spring-boot:run`
   The database `agroconnect` and all tables are created automatically; 19 fruit categories are loaded on first run;
   the admin account is created if `ADMIN_EMAIL`/`ADMIN_PASSWORD` are set.
3. Open http://localhost:8080/ - health check: http://localhost:8080/api/health
4. Tests: `mvn clean test` (see "Tests" below).

## Roles and pages
| Role | Pages |
|---|---|
| Farmer | `farmer.html` (find merchants, my fruit listings with photos), `meetings.html`, `deals.html`, `profile.html?id=`, `myprofile.html` |
| Merchant | `merchant.html` (my requirements, find farmers), `meetings.html`, `deals.html`, `profile.html?id=`, `myprofile.html` |
| Admin | `admin.html` (summary, fruit categories, user accounts, review reports, admin activity log) |
Public: `index.html` (login), `register.html`. All other pages and APIs are protected on the **backend** by role.

## What it does
* **Accounts:** register/login/logout (BCrypt, session cookie), language saved per account, edit own profile.
* **Discovery:** fruit category + ACTIVE status are hard filters; then closeness (same village/PIN, taluka, district,
  nearby district, state), price and rating. Ratings, deal counts and kg are always calculated from stored records.
* **Listings & photos:** kg stored canonically (tons converted at the edge), up to 5 photos, JPG/PNG/WEBP content-checked, 5 MB max.
* **Meetings:** request / accept / reject / propose another time / cancel / complete. Phone numbers appear **only** while a
  CONFIRMED meeting or an open deal exists between that farmer and merchant (`ContactAccessService`).
* **Deals:** the other person must **accept** a proposed deal; deliveries are recorded by one side and confirmed by the other;
  payments are records (never "Verified"); total = kg x price, remaining kg and outstanding rupees are separate numbers;
  completion is a **request the other person must accept**; reviews only after completion, one per person per deal.
* **Admin:** manage fruit categories; activate/deactivate user accounts (takes effect immediately, admins protected);
  moderate reported reviews (hide / restore / dismiss / resolve); every sensitive action goes to an audit log.

## Project layout
```
src/main/java/com/agroconnect/
  config/      DataSeeder, AdminSeeder, WebConfig        controller/  REST endpoints (thin)
  dto/         request/response records (no password hashes)   service/  business rules
  entity/      JPA entities (13 tables)                  repository/  Spring Data interfaces
  security/    SecurityConfig, user details              exception/   ApiException + JSON errors
src/main/resources/static/   HTML, css/style.css, js/app.js, js/translations.js (EN/MR/HI), images/
src/test/java/               JUnit 5 + Mockito unit tests
docs/INTERVIEW_NOTES.md      design decisions and viva Q&A       MANUAL_TEST_CHECKLIST.md
```
Tables: users, fruit_categories, produce_listings, produce_images, merchant_requirements, appointments, deals,
supply_records, payment_records, reviews, complaints, deal_approvals (Phase 6), audit_logs (Phase 7).

## Upgrading from Phase 6
Nothing is deleted or rewritten. On first start Hibernate (`ddl-auto=update`) adds the new table `audit_logs` and two
nullable columns on `complaints` (`resolved_by`, `resolved_at`). Back up first if you like:
`mysqldump -u root -p agroconnect > backup.sql`

## Tests
`mvn clean test` runs 37 unit tests (JUnit 5 + Mockito, no database needed):
`DealApprovalTest` (17: two-party deal confirmation and completion), `AdminServiceTest` (8: account and report moderation),
`ContactAccessServiceTest` (5: phone-number privacy rule), `LocationMatcherTest` (7: location ranking).
These are **unit** tests. There are no automated browser or database integration tests; use `MANUAL_TEST_CHECKLIST.md`
for end-to-end, security, language and mobile checks.

## Known limitations (be honest about these in a review)
* CSRF protection is switched off (session cookie is HttpOnly + SameSite=Lax). Turn on CSRF tokens before real deployment.
* Payment entries are user-entered records; there is no payment gateway and no verification process, so nothing is "Verified".
* No password reset, email verification or phone OTP. No in-app chat or notifications beyond on-screen pending counts.
* "Nearby districts" is a small hand-made Maharashtra list (`DistrictProximity.java`); no maps or geocoding.
* Photos are stored on local disk (`uploads/`) behind a `StorageService` interface so cloud storage can replace it.
* Avatars are placeholder illustrations (`images/avatars/`), labelled as illustrations - not photos of people.
* Supply and payment can still be added while a deal-completion request is waiting.

## Troubleshooting
* `Access denied for user 'root'` - `DB_PASSWORD` not set in the terminal that runs Maven (reopen it after `setx`).
* `Port 8080 already in use` - stop the other app or run with `-Dserver.port=8081`.
* Red errors on `getName()`/`setX()` in the IDE - install/enable the Lombok plugin and annotation processing.
* Photos not showing - the app must be run from the project folder so `./uploads` exists and is writable.

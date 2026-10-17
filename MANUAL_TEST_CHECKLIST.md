# AgroConnect - manual test checklist
Tick each line yourself and note failures. Nothing here has been run by the author of the code.
Use two browsers (or one normal + one private window) for farmer and merchant, a third for the admin.

## A. Setup
- [ ] `mvn clean test` passes (37 unit tests). - [ ] `mvn spring-boot:run` starts; `/api/health` answers.
- [ ] `SHOW TABLES;` shows 13 tables; `SELECT COUNT(*) FROM fruit_categories;` = 19 (or more).
- [ ] Console prints "Admin account created" when ADMIN_EMAIL/ADMIN_PASSWORD are set.

## B. End-to-end demo (the 16 steps of the project brief)
1. [ ] Register a farmer (Nashik) and log in.  2. [ ] Choose Marathi; it stays after logout/login.
3. [ ] Create a Grapes listing (5 tons, Rs 40, 2 photos). Card shows "5,000 kg (5 tons)".
4. [ ] Register a merchant (Nashik) with a Grapes requirement (10 tons, Rs 38-45).
5. [ ] Farmer selects Grapes: the merchant appears, "Same village/district" badge, no phone shown.
6. [ ] Open the merchant profile: "No ratings yet", 0 deals, active requirement, illustrated-avatar label.
7. [ ] Farmer sends a meeting request. 8. [ ] Merchant accepts it.
9. [ ] Phone numbers visible to both **only now**; cancel the meeting - numbers disappear again.
10. [ ] Create a deal (10 tons, Rs 40): estimate shows Rs 4,00,000; the other person must **Accept** it.
11. [ ] Record deliveries 1,500 + 2,000 + 1,000 kg (other side confirms each): remaining 5,500 kg, delivered 4,500 kg.
12. [ ] Record a Rs 1,00,000 payment. 13. [ ] Total Rs 4,00,000, payments Rs 1,00,000, outstanding Rs 3,00,000, remaining kg unchanged.
14. [ ] Request deal completion; other person accepts; deal Completed; both can review once.
15. [ ] Switch English / Marathi / Hindi on every page; names, prices and amounts do not change language.
16. [ ] A third user cannot see these deal/payment records (try `/api/deals`, and POST to `/api/deals/<id>/confirm`: 403).

## C. Two-party approvals (Phase 6)
- [ ] Proposer has no Accept/Reject on own deal; backend returns 403 if called directly. - [ ] Reject -> deal Cancelled, reason shown in history.
- [ ] Completion request keeps the deal Active until accepted; requester cannot accept; reject leaves it Active.

## D. Admin (Phase 7)
- [ ] Summary counts match the database. - [ ] Add / deactivate a fruit category; farmer's chips update.
- [ ] Users table lists farmers/merchants only; search and role filter work.
- [ ] Deactivate a logged-in user: their next click returns to login; login shows the "deactivated" message. Reactivate: login works.
- [ ] The admin account has no Deactivate effect (button returns "cannot be changed here").
- [ ] A user reports a review; admin sees it; **Hide review** removes it from the profile and from the average rating; **Restore review** brings it back.
- [ ] A closed report cannot be actioned twice. - [ ] Each action appears in the activity log with name, time and target.

## E. Security
- [ ] Logged out: `/farmer.html`, `/admin.html`, `/api/deals`, `/uploads/<file>` all redirect/401.
- [ ] Farmer opening `/admin.html` or `/api/admin/users`: redirected / 403. - [ ] Merchant calling `/api/listings/mine`: 403.
- [ ] `SELECT password_hash FROM users;` shows `$2a$...` hashes only. - [ ] `/api/profiles/<id>` JSON contains no email/PIN, and phone only when allowed.
- [ ] Negative quantity / price / payment, or over-quantity delivery, is refused by the server (use the browser console, not the form).
- [ ] Two browser tabs submitting deliveries at the same time cannot exceed the agreed quantity.

## F. Mobile and language
- [ ] At 375 px width: no sideways page scroll, buttons easy to tap, tables scroll inside their own box.
- [ ] Marathi and Hindi text is not clipped on cards, forms, deals and admin pages.
- [ ] No red errors in the browser console (F12) and no stack traces in the Maven console during normal use.

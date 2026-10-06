# Implementation Plan

## General Rule

Implement one numbered sub-phase at a time. Each sub-phase is a bounded task that
must leave the project runnable and tested. A coding session should stop after
its sub-phase's exit check, record what remains, and start the next sub-phase in
a fresh task. Split a sub-phase again if its scope proves too large; do not skip
its tests or combine unrelated sub-phases to fit a session.

When asked to "start Phase 7" (or another future phase), start only its first
incomplete numbered sub-phase. Do not interpret that request as authorization to
implement the whole phase in one task.

Scope update approved 20 September 2026: student QR cards and optional card email
belong to Phase 6, supervised QR attendance to Phase 8, and optional receipt email
to Phase 10. The roadmap remains 15 phases. Phase 5 is complete, Phase 6 is
in progress, and Phases 7–15 remain. See the
[student cards, QR attendance, and delivery plan](STUDENT_CARDS_QR_AND_DELIVERY_PLAN.md).

Do not begin the next phase until:

- The current phase builds
- Important tests pass
- Documentation is updated
- Changes are reviewed
- A Git commit is created

For each sub-phase, complete its focused tests, update affected documentation,
and record the result before continuing. The phase-level gate above still
applies after the final sub-phase. These are scope boundaries, not calendar
deadlines or estimates.

## Phase 1 — Project Foundation

Create:

- Repository structure
- Angular frontend
- Spring Boot backend
- PostgreSQL Docker Compose
- Flyway setup
- Environment configuration
- Health endpoints
- Initial frontend shell
- Logo and theme integration
- README development instructions
- Basic CI-ready build commands

Do not implement business modules.

Deliverables:

- Frontend starts
- Backend starts
- Database starts
- Health check works
- Builds and tests pass

## Phase 2 — Authentication and Authorization

Implement:

- Users
- Roles
- User-branch assignments
- Login
- Access tokens
- Refresh tokens
- Logout
- Password change
- User status
- Backend authorization
- Angular login and guards
- Role-based navigation

Seed:

- Roles
- Default branch
- Safe first-super-admin setup

Tests:

- Login
- Token refresh
- Disabled user
- Role restrictions
- Branch restrictions

## Phase 3 — Branch and User Administration

Implement:

- Branch list and form
- User list and form
- Role assignments
- Branch assignments
- Lecturer account creation
- User activation/deactivation
- Audit events

Phase 3 implementation note:

- `SUPER_ADMIN` manages branches and all user roles.
- `ADMIN` manages lecturer accounts only within assigned branches.
- Audit events are persisted in `audit_logs`; the audit viewer remains a later phase.

## Phase 4 — Courses and Batches

Implement:

- Course CRUD
- Batch CRUD
- Batch statuses
- Lecturer assignment
- Fee-plan configuration
- Schedule-mode selection
- Batch wizard UI

Do not generate student charges yet.

## Phase 5 — Student Management

Status: Completed on 2026-07-21. Enrollment-derived Student filters, counts,
and lecturer visibility remain part of Phase 6.

Implement:

- Student CRUD
- NIC normalization and uniqueness
- Student search
- Photo upload
- Student profile
- No destructive deletion of historical students

## Phase 6 — Enrollment, Charge Generation, and Student Cards

Status: In progress. Enrollment and atomic charge creation are implemented with
migration V9. One revocable student-level QR card, two-sided printable PDF,
card history, and branch-authorized issue/replacement/cancellation are implemented
with migration V10. Optional staff-confirmed card email, durable delivery status,
idempotent queuing, bounded retries, and delivery audit are implemented with
migration V11; sending needs a configured school SMTP account. Audited enrollment
status changes are implemented. Lecturers can now view a minimal student roster
for currently assigned batches; detailed student profiles and financial records
remain administrator-only.
Bundled Sinhala and Tamil card-name fonts are implemented and sample PDFs were
visually checked. Printed card/scanner compatibility and real SMTP delivery
still need school-side validation before rollout. Do not mark Phase 6 complete
until these are done. Use the [Phase 6 rollout checklist](PHASE6_ROLLOUT_CHECKLIST.md)
for the physical and SMTP checks.

Implement:

- Enrollment
- Server-side registration-number generation
- Duplicate-enrollment prevention
- Fee snapshot
- Registration charge
- Monthly installments
- Examination charge
- Transaction rollback
- Student and batch enrollment views
- Student-level QR card issuance after enrollment; one active card across courses
- Branded card preview, printable PDF, physical handover, and download
- Card cancellation/replacement with old-token revocation and audit history
- Optional staff-triggered card PDF email to the checked saved student address
- Shared email configuration, durable delivery status, retry/resend, and audit foundation

This phase requires strong integration and concurrency tests.
Implement enrollment/charges first, then cards, then email. Card rendering and
delivery failures must not roll back enrollment or generated charges. Photos and
email remain optional. Final card design and token/artifact protection follow the
[detailed plan](STUDENT_CARDS_QR_AND_DELIVERY_PLAN.md).

Remaining school-side sub-phases (no new product feature is implied):

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 6.R1 — Physical card validation | Print a synthetic sample at actual card size; check the photo, multilingual name, front/back alignment, and QR with the intended phone and USB 2D scanner. | Record the printer, stock, scanner, and any design corrections using the [rollout checklist](PHASE6_ROLLOUT_CHECKLIST.md). |
| 6.R2 — School email validation | Configure school SMTP outside Git; send a synthetic card PDF, test receipt and a controlled failure/retry. | Record provider acceptance and inbox result separately; verify print/download still work with email disabled. |

Both checks require school equipment or account access. Record the exact blocker
if either cannot be performed; do not claim Phase 6 rollout is complete from
automated tests alone.

## Phase 7 — Class Schedules and Sessions

Status: Phase 7.1 implemented on 4 October 2026; validation results are recorded in
[the session foundation report](PHASE7_1_SESSION_FOUNDATION.md). V12 introduces
weekly-pattern and session persistence; session list/detail APIs enforce current
branch/batch-assignment access.
Phase 7.2 implemented on 4 October 2026: weekly-pattern APIs, bounded signed
preview, atomic generation, V13/V14 origin and duplicate constraints, and
local-time persistence correction. See [the 7.2 report](PHASE7_2_REGULAR_GENERATION.md).
Phase 7.3 implemented on 4 October 2026: manual/extra session create/edit APIs,
current lecturer/branch authorization, overlap and stale-version protection,
shared batch locking, and transactional audit. See [the 7.3 report](PHASE7_3_MANUAL_SESSIONS.md).
Phase 7.4 implements reason/version-checked cancellation and atomic rescheduling,
preserved original/replacement chains, V15 integrity constraints, audit and an
attendance-state domain guard. See [the 7.4 report](PHASE7_4_SESSION_LIFECYCLE.md).
Phase 7.5 implemented on 4 October 2026: admin weekly-pattern editing, bounded
preview with holiday exclusions, conflict review and confirmed generation in a
responsive Angular screen. See [the 7.5 report](PHASE7_5_REGULAR_SCHEDULE_UI.md).
Phase 7.6 implemented on 5 October 2026: admin month calendar, paginated list,
manual/extra creation, editing, cancellation and rescheduling UI. See
[the 7.6 report](PHASE7_6_SESSION_MANAGEMENT_UI.md).
Phase 7.7 implemented on 5 October 2026: lecturers can use Class sessions for
currently assigned batches, active colleague assignments can be read without
opening assignment writes, and schedule/lifecycle regressions passed. See
[the 7.7 report](PHASE7_7_LECTURER_SESSIONS.md).
Next: Phase 8 — Attendance and QR Scanning. Phase 6 school-side
rollout checks remain open; the user's request to start Phase 7 advances development
without claiming those physical/email checks are complete. The user reported SMTP
ACCEPTED; inbox/PDF receipt, controlled retry and physical equipment checks still
need recorded evidence.

Implement:

- Regular weekly schedule patterns
- Manual schedule mode
- Session preview
- Bulk generation
- Duplicate detection
- Extra session creation
- Cancellation
- Rescheduling
- Session calendar and list
- Lecturer session access

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 7.1 — Session foundation | Add session and weekly-pattern schema, validation, and branch/lecturer-aware list/detail APIs. | Migration, role/branch tests, and a readable session response pass. |
| 7.2 — Regular generation | Add weekly patterns, bounded preview, bulk generation, and duplicate prevention. | Preview and saved sessions match within batch dates; repeat generation creates no duplicates. |
| 7.3 — Manual sessions | Add one-off/manual session creation and edit rules, including extra sessions in regular batches. | Authorized users can create and edit valid sessions; invalid overlaps and dates are rejected. |
| 7.4 — Session lifecycle | Add cancellation and rescheduling with reason and original-session traceability. | History is retained; cancelled sessions cannot later receive attendance. |
| 7.5 — Regular schedule UI | Add responsive weekly-pattern editing, preview, and generation controls. | Admin can review and generate regular sessions with loading/error states. |
| 7.6 — Session management UI | Add session list/calendar, manual create/edit, cancel, and reschedule controls. | Admin completes the manual and lifecycle flows from the UI. |
| 7.7 — Lecturer view and regression | Show only assigned sessions and finish end-to-end schedule checks. | Lecturer scope, duplicate handling, and cancellation/reschedule flows pass. |

## Phase 8 — Attendance and QR Scanning

Implement:

- Attendance sheet
- Every attendance write must take the shared batch lock, reload the session and
  call `requireAttendanceEligible()` before saving; cancelled/rescheduled sessions
  are ineligible. Add attendance-versus-cancellation race tests. Extend lifecycle
  guards to detect saved attendance rows, including drafts, before implementing
  the confirmed/audited correction flow.
- Mark All Present
- Present, Absent, Late, Excused
- Save and submit
- Overwrite confirmation
- Completion state
- Overdue warning with restricted financial detail
- Operational attendance summaries and incomplete-sheet view
- Scan student cards in an explicitly selected session using a staff phone camera
  or USB 2D QR scanner; support both regular and manual sessions
- Server-side card, enrollment, branch, and lecturer-assignment checks
- Duplicate-safe scan saving, revoked-card rejection, and visible save results
- Name/photo display for supervised identity checks; manual attendance backup
- Online-only first release, clear failure handling, and paper fallback during outages

Use Phase 6 card credentials and Phase 7 sessions. Overdue fees never block a scan.
Scans must not silently overwrite attendance or infer absence for unscanned students.

Pilot guidance follow-up (recorded 3 October 2026): before the proposed pilot
after Phase 8, prioritize role-specific in-app Help, a short illustrated enrollment
guide, and a lecturer guide covering session selection, QR/manual attendance,
and camera/network fallbacks. Train one school administrator to support staff,
then observe staff completing tasks with test records and improve confusing
screens or guidance. Include contextual workflow instructions and a first-use
administrator setup checklist in the guidance plan. Scope this follow-up as
bounded work after the attendance sub-phases; it is not implemented by this note.
See decision 27 in [Decisions and Assumptions](DECISIONS_AND_ASSUMPTIONS.md).

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 8.1 — Attendance foundation | Add attendance schema, unique enrollment/session rule, eligibility checks, and an authorized session roster API. | Ineligible and cancelled sessions are rejected; lecturer/branch tests pass. |
| 8.2 — Manual marking | Add the responsive sheet, individual statuses, late details, and Mark All Present. | Staff can mark each eligible student without a scanner; validation and role tests pass. |
| 8.3 — Submission and corrections | Add save/submit, completion state, overwrite confirmation, and historical audit. | Duplicate records cannot be created and corrections are traceable. |
| 8.4 — Warnings and summaries | Add overdue warning with lecturer-safe total and operational attendance summaries. | Overdue never blocks marking; restricted finance remains hidden. |
| 8.5 — QR scan API | Resolve active card tokens within a selected session; validate card, enrollment, branch, assignment, and duplicate/retry behavior. | Revoked/wrong-batch/unauthorized scans fail safely; successful scans save once with actor and time. |
| 8.6 — QR scan UI | Add staff phone-camera and USB 2D scanner input, identity display, save feedback, and manual/paper fallback. | Real device checks show success only after server save; denied camera/network failure has a usable fallback. |
| 8.7 — Attendance acceptance | Add attendance percentages, absent/late/incomplete-sheet views, and the full manual-plus-scan workflow tests. | Cancelled/excused calculations are stated clearly; Phase 8 acceptance criteria pass. |

The broader exportable attendance report catalogue belongs to Phase 12; Phase 8
provides the operational views needed to complete attendance safely.

## Phase 9 — Discounts and Waivers

Implement:

- Fixed discount
- Percentage discount
- Full waiver
- Approval and reason
- Charge recalculation
- Audit history
- Paid-charge restrictions

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 9.1 — Adjustment rules | Add adjustment records, final-payable calculation, reason/approver validation, paid-charge restriction, and audit. | Fixed, percent, full-waiver, boundary, and authorization tests pass. |
| 9.2 — Adjustment API | Add authorized create/list/detail operations and consistent charge status/amount updates. | Original and final amounts remain traceable; concurrent edits cannot corrupt a charge. |
| 9.3 — Admin workflow | Add a charge-level apply/review UI with clear approval, reason, and error states. | Admin can perform each adjustment; lecturers see no controls or financial details. |

## Phase 10 — Payments, Receipts, and Delivery

Implement:

- Unpaid-charge selection
- No partial settlement
- Multi-charge payment
- Payment methods
- Allocation
- Receipt numbering
- Printable receipt
- PDF receipt
- Optional staff-triggered receipt PDF email, reusing Phase 6 delivery infrastructure
- Checked saved recipient, delivery status, retry/resend, and delivery audit history
- Payment/receipt durability independent of delivery failure; void-aware resends
- Payment history
- Voiding and reversal
- Audit history

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 10.1 — Payment transaction | Add payment/allocation schema and server-side full-charge settlement, method validation, and unique receipt numbering. | Atomic single/multi-charge payments pass; partial or mismatched allocations roll back. |
| 10.2 — Payment UI and history | Add charge selection, amount preview, posting, and authorized enrollment payment history. | UI cannot submit partial settlement; saved payments and charges reconcile. |
| 10.3 — Voiding | Add authorized reason-required voiding, allocation reversal, restored charge states, and audit. | Voids cannot delete history or double-reverse allocations; tests pass. |
| 10.4 — Receipt PDF | Add protected receipt detail, print, download, and regeneration tracking. | One receipt number remains tied to the payment; printed/PDF content is verified. |
| 10.5 — Receipt email | Reuse the Phase 6 delivery service for checked saved addresses, status, retries, and explicit resend. | Failed delivery leaves payment/receipt intact; voided-receipt and lecturer restrictions pass. |

## Phase 11 — Dashboards

Implement:

- Administrator dashboard
- Lecturer dashboard
- Branch-aware metrics
- Attendance metrics
- Financial metrics
- Recent activity

Use database aggregations and avoid inefficient loading.

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 11.1 — Admin academic metrics | Add branch-aware aggregation for active students/batches and today's sessions/attendance. | Academic counts match source records and enforce branch scope. |
| 11.2 — Admin finance metrics | Add fee collection, outstanding/overdue, and recent-payment aggregates. | Totals reconcile with payments and charge state without loading full lists. |
| 11.3 — Lecturer metrics | Add assigned-batch and session aggregates without restricted finance details. | Lecturer data contains only authorized batches and allowed warning totals. |
| 11.4 — Dashboard UI | Build responsive admin/lecturer dashboards with loading, empty, error, and date context. | Each role sees its own metrics; realistic-data response time is checked. |

## Phase 12 — Reports and Exports

Implement all required attendance and financial reports.

Add:

- Filters
- Pagination
- PDF export
- Excel export
- Print layouts
- Export audit where required

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 12.1 — Attendance core reports | Add daily sheet, by-batch, and monthly summary with branch/assignment filters and pagination. | Counts reconcile with completed eligible sessions; lecturer scope passes. |
| 12.2 — Student and absence reports | Add individual student, absent, and late reports. | Status and date filters reconcile with attendance records. |
| 12.3 — Attendance quality reports | Add percentage and incomplete-session reports. | Excused/cancelled handling is explicit and verified. |
| 12.4 — Student fee reports | Add payment history, outstanding balance, and overdue reports. | Totals reconcile with charges, adjustments, payments, and voids. |
| 12.5 — Collection totals | Add batch and monthly fee collection reports. | Date and branch totals reconcile to settled payments. |
| 12.6 — Collection breakdowns | Add registration/exam fee, payment method, and daily cashier reports. | Category and cashier totals reconcile to the same payments. |
| 12.7 — Finance exception reports | Add voided-payment and discounts/waivers reports with restricted access. | Audit references and totals are correct; lecturers cannot open them. |
| 12.8 — Exports and print | Add bounded PDF/Excel export, readable print layouts, and export audit where required. | Exports match filtered on-screen totals without loading unbounded data. |

## Phase 13 — Audit and Operational Controls

Implement:

- Audit viewer
- Sensitive-action search
- Receipt regeneration tracking
- Attendance-correction visibility
- Payment-void visibility
- Card issuance/replacement/revocation and document-delivery audit visibility

Audit logs remain read-only.

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 13.1 — Audit query | Add paginated, authorized audit APIs with branch, actor, action, entity, and date filters. | Sensitive records are scoped correctly and immutable. |
| 13.2 — Audit viewer | Add responsive read-only search and event detail screens. | Admin can trace before/after and reason without editing events. |
| 13.3 — Operational trails | Surface receipt regeneration, attendance corrections, payment voids, card changes, and delivery outcomes. | Each sensitive workflow links to its audit evidence. |

## Phase 14 — Security and Quality Review

Perform:

- Authorization review
- Branch-isolation review
- Financial transaction review
- File-upload review
- Dependency review
- Logging review
- Error-handling review
- Accessibility review
- Mobile usability review
- Performance tests
- QR replay/duplicate handling, credential revocation, and scan authorization review
- Printed/digital QR readability and staff-phone/scanner workflow checks
- Email recipient, delivery retry, secret handling, and voided-receipt review

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 14.1 — Access review | Recheck role, branch, assignment, and direct-object access across APIs. | Security tests cover unauthorized reads and writes. |
| 14.2 — Financial integrity | Review transactions, rounding, reversals, concurrency, and audit. | Failure/retry tests leave charges and payments consistent. |
| 14.3 — Card, file, and email security | Review uploads, QR replay/revocation, protected artifacts, recipients, retry, and secrets. | Rejected scans and delivery failures leak no restricted data or stale document. |
| 14.4 — Usability and accessibility | Review mobile grids, keyboard flow, labels, and error handling. | Critical staff screens pass phone-width and keyboard checks. |
| 14.5 — Performance and observability | Review logging and realistic data/load behavior. | Common screens and reports meet agreed response targets with actionable logs. |
| 14.6 — End-to-end release check | Run synthetic student-to-attendance-to-payment and printed/digital QR scenarios. | Critical flows pass with the chosen printer, scanner, and school email setup. |

## Phase 15 — Deployment Preparation

Create:

- Production Dockerfiles
- Deployment guide
- Environment-variable guide
- Database backup guide
- Restore guide
- HTTPS/reverse-proxy guide
- Initial admin setup guide
- Monitoring recommendations
- Release checklist
- School sender/provider configuration and optional-email setup guide
- Card printing/scanner setup and camera-permission troubleshooting
- Delivery-failure monitoring, protected card artifacts, and recovery procedures

| Sub-phase | Bounded task | Exit check |
| --- | --- | --- |
| 15.1 — Production packaging | Add production Dockerfiles and environment configuration without secrets. | Images build and the stack starts with documented variables. |
| 15.2 — Database recovery | Write and rehearse backup, restore, and migration procedures. | A synthetic backup restores successfully in a clean environment. |
| 15.3 — Network and monitoring | Document HTTPS/reverse proxy, health checks, logs, alerts, and delivery-failure monitoring. | Deployed test environment uses HTTPS and exposes actionable health signals. |
| 15.4 — School operations | Document initial admin, sender/provider, card printing/scanning, camera permission, protected files, and recovery. | School staff can follow the runbooks and release checklist without developer intervention. |

## Phase Completion Report

For every sub-phase and phase, Codex must report:

- Summary
- Changed files
- Migrations
- Tests
- Commands run
- Results
- Assumptions
- Known limitations
- The exact next sub-phase (or an external blocker) and its starting point

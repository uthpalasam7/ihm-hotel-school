# Implementation Plan

## General Rule

Implement one phase at a time. Each phase must leave the project runnable and tested.

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

## Phase 7 — Class Schedules and Sessions

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

## Phase 8 — Attendance and QR Scanning

Implement:

- Attendance sheet
- Mark All Present
- Present, Absent, Late, Excused
- Save and submit
- Overwrite confirmation
- Completion state
- Attendance summaries
- Overdue warning with restricted financial detail
- Attendance reports
- Scan student cards in an explicitly selected session using a staff phone camera
  or USB 2D QR scanner; support both regular and manual sessions
- Server-side card, enrollment, branch, and lecturer-assignment checks
- Duplicate-safe scan saving, revoked-card rejection, and visible save results
- Name/photo display for supervised identity checks; manual attendance backup
- Online-only first release, clear failure handling, and paper fallback during outages

Use Phase 6 card credentials and Phase 7 sessions. Overdue fees never block a scan.
Scans must not silently overwrite attendance or infer absence for unscanned students.

## Phase 9 — Discounts and Waivers

Implement:

- Fixed discount
- Percentage discount
- Full waiver
- Approval and reason
- Charge recalculation
- Audit history
- Paid-charge restrictions

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

## Phase 11 — Dashboards

Implement:

- Administrator dashboard
- Lecturer dashboard
- Branch-aware metrics
- Attendance metrics
- Financial metrics
- Recent activity

Use database aggregations and avoid inefficient loading.

## Phase 12 — Reports and Exports

Implement all required attendance and financial reports.

Add:

- Filters
- Pagination
- PDF export
- Excel export
- Print layouts
- Export audit where required

## Phase 13 — Audit and Operational Controls

Implement:

- Audit viewer
- Sensitive-action search
- Receipt regeneration tracking
- Attendance-correction visibility
- Payment-void visibility
- Card issuance/replacement/revocation and document-delivery audit visibility

Audit logs remain read-only.

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

## Phase Completion Report

For every phase, Codex must report:

- Summary
- Changed files
- Migrations
- Tests
- Commands run
- Results
- Assumptions
- Known limitations
- Next recommended phase

x# Implementation Plan

## General Rule

Implement one phase at a time. Each phase must leave the project runnable and tested.

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

Implement:

- Student CRUD
- NIC normalization and uniqueness
- Student search
- Photo upload
- Student profile
- No destructive deletion of historical students

## Phase 6 — Enrollment and Charge Generation

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

This phase requires strong integration and concurrency tests.

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

## Phase 8 — Attendance

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

## Phase 9 — Discounts and Waivers

Implement:

- Fixed discount
- Percentage discount
- Full waiver
- Approval and reason
- Charge recalculation
- Audit history
- Paid-charge restrictions

## Phase 10 — Payments and Receipts

Implement:

- Unpaid-charge selection
- No partial settlement
- Multi-charge payment
- Payment methods
- Allocation
- Receipt numbering
- Printable receipt
- PDF receipt
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

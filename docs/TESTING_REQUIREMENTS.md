# Testing Requirements

## 1. Testing Strategy

Use:

- Backend unit tests
- Backend integration tests
- Repository tests
- Security tests
- Frontend unit and component tests
- End-to-end tests
- Manual acceptance tests
- Build verification

Do not mark a phase complete while important tests are failing.

## 2. Backend Unit Tests

Test business services for:

### Students

- NIC normalization
- NIC required
- Duplicate NIC rejection
- Existing-student lookup
- Student list pagination and search
- Administrator and lecturer role restrictions
- Unauthorized active-branch header rejection
- Student create, update, status, and photo audit events
- JPEG/PNG signature, MIME, size, dimension, thumbnail, replacement, and deletion behaviour

### Courses and Batches

- Unique course short code
- Unique batch number
- Date validation
- Positive duration
- Branch assignment validation

### Registration Numbers

- Correct format
- Four-digit padding
- Sequence increment
- No reuse
- Concurrent enrollment safety

### Session Generation

- Weekly date generation
- Multiple weekly patterns
- Date-range boundaries
- End-date inclusion
- Excluded dates
- Invalid time ranges
- Duplicate prevention
- Manual session creation
- Cancellation rules
- Rescheduling traceability

### Attendance

- One record per enrollment and session
- Mark all present
- Cancelled session blocking
- Ineligible enrollment blocking
- Late validation
- Attendance-percentage calculation
- Excused exclusion
- Overdue warning does not block attendance
- QR lookup and revoked/unknown-card rejection
- Session, branch, assignment, and enrollment validation for scans
- Duplicate/retried/concurrent scans preserve one attendance record and its status
- QR capture records staff actor and check-in time; scan does not imply all-class attendance

### Student Cards and Document Delivery — Planned Phases 6 and 10

- One active card per student, shared across enrollments
- Concurrent issuance/replacement, old-token revocation, and historical traceability
- Reprint/download retains the active credential; QR and printed fields exclude NIC/fees
- Printable PDF renders Latin, Sinhala, and Tamil student names and preserves a
  decodable QR after the PDF back is rasterized at a realistic print resolution
- Optional photo and email; missing email does not block core workflows
- Administrator/branch authorization on card and delivery actions
- Card rendering/delivery failure does not roll back enrollment or charges
- Email failure does not undo payments, allocations, or receipt numbers
- Durable post-commit delivery jobs, request deduplication, bounded retries, and explicit resend
- Recipient snapshot and delivery outcomes; no raw QR/PDF/secrets in logs
- Revoked-card and voided-receipt checks before dispatch/retry

### Fee Generation

- Registration charge
- Correct number of installments
- Examination charge
- Due-day handling
- Month-end handling
- First installment before start-date handling
- Exact installment total
- Final-installment rounding
- Zero-fee handling
- Negative-fee rejection

### Discounts and Waivers

- Fixed discount
- Percentage discount
- Full waiver
- Negative final amount prevention
- Mandatory reason
- Mandatory approver
- Paid-charge adjustment blocking

### Payments

- Full single-charge payment
- Full multi-charge payment
- Partial settlement rejection
- Charge ownership validation
- Allocation total equality
- Duplicate payment race prevention
- Receipt generation
- Payment void
- Allocation reversal
- Charge-status restoration
- Mandatory void reason

## 3. Backend Integration Tests

Use PostgreSQL, preferably through Testcontainers.

Test:

- Flyway migrations
- Unique constraints
- Foreign keys
- Transaction rollbacks
- Optimistic locking
- Registration-number concurrency
- Payment concurrency
- Branch-filtered repositories
- Attendance bulk submission
- Report queries

## 4. Security Tests

Verify:

- Unauthenticated requests are rejected.
- Lecturer cannot access unassigned batch.
- Lecturer sees a minimal roster only for a current active assignment in an
  authorized branch; expired assignments and financial/NIC details stay hidden.
- Lecturer cannot access financial endpoints.
- Lecturer sees only reduced overdue data.
- Admin cannot access unauthorized branch.
- Super administrator can access all branches.
- Disabled users cannot refresh tokens.
- Cross-branch entity IDs are rejected.
- Export endpoints apply the same permissions.

## 5. Frontend Tests

Test:

- Login validation
- Role-based menu visibility
- Branch auto-selection for one branch
- Student NIC duplicate handling
- Batch wizard validation
- Fee installment preview
- Session generation preview
- Attendance mark-all-present
- Late field visibility
- Overdue warning
- Attendance remains enabled with overdue fees
- Full-charge-only payment selection
- Payment total calculation
- Void confirmation
- Report filters
- API error display
- Mobile layout for critical screens
- Card preview/print layout with and without photo, including QR clear space
- Camera permission denied/unavailable, scanner focus, and keyboard-only scan workflow
- Explicit session selection, name/photo result, duplicate/error/success messages
- No false success before server confirmation; manual fallback after network failure
- Optional email recipient review, unconfigured-service state, and failed-delivery retry

## 6. End-to-End Scenarios

### Scenario A: Core Setup

1. Super admin logs in.
2. Default branch exists.
3. Admin and lecturer users are created.
4. Lecturer is assigned to the branch.

### Scenario B: Course and Batch

1. Admin creates Pastry & Bakery.
2. Admin creates batch `2026/PB02`.
3. Admin configures a six-month fee plan.
4. Admin selects monthly due day.
5. Admin sets examination due date.
6. Admin assigns a lecturer.

### Scenario C: Sessions

1. Admin creates Monday and Wednesday schedules.
2. System previews sessions.
3. Admin generates sessions.
4. Admin cancels one session.
5. Admin creates an extra session.
6. Cancelled session is excluded from attendance totals.

### Scenario D: Enrollment

1. Admin creates a student with mandatory NIC.
2. Admin enrolls the student.
3. Registration number is generated.
4. Registration, monthly, and examination charges are generated.
5. Same student is enrolled in a different batch.
6. Duplicate enrollment in the same batch is rejected.
7. Two concurrent enrollments get distinct sequential numbers; concurrent
   duplicates create one enrollment only.
8. A charge insert failure rolls back the enrollment, sequence, and audit.
9. Fee edits leave prior snapshots and charges unchanged.
10. A changed preview version requires the administrator to preview again.

### Scenario E: Attendance

1. Lecturer logs in.
2. Lecturer sees only assigned batch.
3. Lecturer opens today's session.
4. Lecturer marks all present.
5. One student is changed to Late.
6. Attendance is submitted.
7. Unauthorized batch access is rejected.

### Scenario F: Fees and Payment

1. Admin views generated charges.
2. Partial payment is not available.
3. Admin selects full charges.
4. System calculates total.
5. Admin records payment.
6. Receipt is generated.
7. Paid charges change to Paid.

### Scenario G: Overdue Warning

1. An unpaid charge passes its due date.
2. Charge becomes Overdue.
3. Lecturer sees warning and total overdue amount.
4. Lecturer can still mark attendance.
5. Lecturer cannot access detailed payment history.

### Scenario H: Discounts and Voids

1. Admin applies an approved discount with reason.
2. Final amount updates.
3. Admin records full payment.
4. Authorized admin voids the payment with reason.
5. Allocations reverse.
6. Charge status returns correctly.
7. Audit records exist.

### Scenario I: Student Card and QR Attendance — Phases 6 and 8

1. Enroll one student in two different batches and issue one student-level card.
2. Preview and print/download the card; verify a no-photo variant also works.
3. Scan the physical card using a supported staff phone and USB 2D QR scanner.
4. Select a session in each enrolled batch and verify attendance is recorded only
   for the selected enrollment/session, with name and available photo displayed.
5. Scan twice, retry after a lost response, and submit simultaneous scans; confirm
   exactly one record and no silent overwrite of existing Late/Excused status.
6. Replace the card; confirm the old QR is rejected and the new one works.
7. Reject wrong-batch, ineligible-enrollment, cancelled-session, and unauthorized scans.
8. Verify overdue warnings do not block scanning; manually mark a student who forgot
   their card and review unscanned students before final submission.
9. Deny camera permission and interrupt network access; verify useful fallback and
   no false saved indication. Test a digital card on supported scanning equipment.

### Scenario J: Card and Receipt Email — Phases 6 and 10

1. Configure a test mail service; use synthetic recipients and documents only.
2. Explicitly email a card PDF to the checked saved student address; inspect status.
3. Post a valid payment and email its receipt without creating a second receipt.
4. Simulate provider failure and repeated requests; retry safely and verify the
   enrollment/payment/allocations remain intact with no duplicate jobs from retries.
5. Intentionally resend and verify a separate audited delivery attempt for the
   same document; distinguish provider acceptance from confirmed delivery.
6. Revoke a queued card or void a payment before retry; do not send an invalid card
   or a receipt PDF that conceals the voided status.
7. Remove email or disable the mail configuration; retain print/download and all
   core enrollment, attendance, and payment operations.
8. Reject lecturer and unauthorized-branch delivery attempts.

## 7. Performance Checks

Verify acceptable response time for:

- Student search
- Attendance-sheet loading
- Dashboard summaries
- Batch student list
- Payment posting
- Common reports

Use realistic data volumes before production.

## 8. Build Checks

Every phase should run:

Backend:

```bash
./mvnw test
./mvnw package
```

Frontend:

```bash
npm test -- --watch=false
npm run build
```

Infrastructure:

```bash
docker compose config
```

## 9. Test Data

Use synthetic data only.

Do not place real student NICs, contact details, or payment information in automated tests or source control.

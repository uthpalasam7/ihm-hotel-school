# AGENTS.md

## 1. Purpose

This repository contains the **IHM Hotel School Management System**, an English-only, internet-accessible web application for managing:

- Branches
- Users and lecturer accounts
- Courses and course batches
- Students and multi-course enrollments
- Class sessions
- Student attendance
- Student fees, discounts, waivers, payments, receipts and overdue warnings
- Attendance and financial reports

The initial school has one physical branch, but the system must support multiple branches from the beginning.

This file defines the mandatory rules for any AI coding agent working in this repository.

---

## 2. Working Style for Coding Agents

Before changing code:

1. Read this file completely.
2. Read the relevant files under `docs/`.
3. Inspect the current implementation before proposing a solution.
4. Do not rewrite working modules unnecessarily.
5. Implement one bounded feature at a time.
6. Explain assumptions when a requirement is unclear.
7. Prefer simple, maintainable solutions over clever abstractions.
8. Keep the application runnable after every completed task.
9. Add or update tests for every important business rule.
10. Update documentation when behaviour, APIs or database structures change.

Do not generate the entire application in one uncontrolled change. Work in phases and keep commits logically separated.

For every completed task, report:

- What changed
- Files changed
- Database migrations added
- Tests added or updated
- Commands run
- Any assumptions or remaining risks

---

## 3. Approved Technology Stack

### Frontend

- Angular
- TypeScript
- Responsive web design
- Angular reactive forms
- Route guards
- HTTP interceptors
- A clean feature-based folder structure

Use a stable Angular version available when the project is created. Do not use an obsolete Angular version for this new system.

### Backend

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway database migrations
- Maven Wrapper

### Authentication

- JWT access tokens
- Refresh tokens
- Secure password hashing
- Role-based and branch-aware authorization

### Deployment

- Docker
- Docker Compose for local development
- Environment-variable-based configuration
- HTTPS in deployed environments

### File Storage

Student photos must be stored through a storage abstraction.

For local development, filesystem storage is acceptable. Production storage must be replaceable with object storage without changing student business logic.

---

## 4. Repository Structure

Use this target structure unless the repository already has an equivalent structure:

```text
ihm-hotel-school/
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── .env.example
├── docs/
│   ├── PRODUCT_SPEC.md
│   ├── BUSINESS_RULES.md
│   ├── DATABASE_DESIGN.md
│   ├── API_SPECIFICATION.md
│   ├── UI_REQUIREMENTS.md
│   ├── SECURITY_REQUIREMENTS.md
│   ├── TESTING_REQUIREMENTS.md
│   └── IMPLEMENTATION_PLAN.md
├── frontend/
└── backend/
```

Do not place backend code in the frontend project or frontend code in the backend project.

---

## 5. Branding and UI Theme

The system is for **IHM Hotel School**.

Use the supplied school logo in the frontend assets folder, for example:

```text
frontend/src/assets/branding/ihm-logo.jpg
```

Base the visual theme on the logo:

- Primary gold: `#B0852C`
- Secondary brown: `#AD6715`
- Charcoal: `#292929`
- Light background: `#FDFDFD`
- Border grey: `#E5E5E5`

Guidelines:

- Use gold for primary actions and selected navigation.
- Use charcoal for headings, navigation and strong text.
- Use white or light grey for large backgrounds.
- Do not overuse gold across large page areas.
- Use accessible contrast and visible focus states.
- The interface must work on desktop, tablet and mobile.
- The application language is English only.

---

## 6. Roles and Authorization

Implement these roles:

### `SUPER_ADMIN`

- Full access to all branches
- Manage branches
- Manage administrators and lecturers
- Access all courses, batches, students, attendance and financial data
- View audit history
- Correct or void sensitive records according to business rules

### `ADMIN`

- Operate within assigned branches
- Manage courses and batches
- Register students
- Enroll students
- Assign lecturers
- Manage class sessions
- Manage fees, payments, discounts, waivers and receipts
- View and export reports
- Edit historical attendance with audit logging

### `LECTURER`

- View only assigned branches and assigned course batches
- View students in assigned batches
- Create or manage sessions only for assigned batches
- Mark attendance for assigned batches
- View attendance reports for assigned batches
- See only a payment warning and overdue amount for students with overdue fees
- Must not view detailed payment history, discounts, waivers or receipts
- Must not create, edit or void financial records

Authorization must be enforced in the backend. Hiding frontend controls is not sufficient security.

---

## 7. Branch Management

The application currently has one branch, but all relevant data must support multiple branches.

Create a default branch during setup:

```text
Name: IHM Hotel School
Code: IHM-MAIN
Status: ACTIVE
```

Branch rules:

- A course batch belongs to one branch.
- Users may be assigned to one or more branches.
- Reports must support filtering by branch.
- Branch-aware users must not access data from unauthorized branches.
- When a user has only one branch, the UI should select it automatically.
- Do not force users to repeatedly choose the only available branch.

---

## 8. Core Domain Model

Use domain names consistently.

Recommended entities:

- `Branch`
- `User`
- `Role`
- `UserBranch`
- `LecturerProfile`
- `Course`
- `CourseBatch`
- `BatchLecturer`
- `Student`
- `Enrollment`
- `BatchSchedule`
- `ClassSession`
- `Attendance`
- `FeePlan`
- `StudentCharge`
- `Payment`
- `PaymentAllocation`
- `DiscountWaiver`
- `Receipt`
- `AuditLog`

Use database foreign keys, indexes and unique constraints. Do not rely only on application validation.

---

## 9. Course Management

A `Course` represents the reusable course definition.

Example:

```text
Course name: Pastry & Bakery
Short code: PB
Description: Certificate course in pastry and bakery
Status: ACTIVE
```

Examples of courses:

- Pastry & Bakery
- Food and Beverage
- Professional Cookery
- Housekeeping

Course rules:

- Course name is required.
- Short code is required and unique.
- A course can have many batches.
- Changing a course must not silently change historical batch data.

---

## 10. Course Batch Management

A `CourseBatch` represents one intake of a course.

Example:

```text
Course: Pastry & Bakery
Batch number: 2026/PB02
Branch: IHM-MAIN
Start date: 2026-07-01
End date: 2026-12-31
Duration months: 6
Status: UPCOMING
```

Supported batch statuses:

- `UPCOMING`
- `ACTIVE`
- `COMPLETED`
- `CANCELLED`

Batch rules:

- Batch number is required and globally unique.
- Start date must not be after end date.
- Duration months must be a positive integer.
- Each batch belongs to exactly one course and one branch.
- Each batch can have one or more assigned lecturers.
- Fees are configured on the batch, not only on the reusable course.
- Existing enrollment charges must not change when batch fee settings are edited later.
- A batch can use either a regular weekly schedule or manual session management.

---

## 11. Student Management

Required student fields:

- Internal ID
- Full name
- NIC
- Contact number
- Alternative contact number, optional
- Email, optional
- Address
- Date of birth, optional unless later made mandatory
- Gender, optional
- Student photo, optional
- Remarks, optional
- Status
- Audit fields

Student rules:

- NIC is mandatory.
- NIC must be unique after normalization.
- Trim spaces and normalize letter casing before uniqueness checks.
- Do not create a second student when an existing NIC is entered.
- The UI must search and display the existing student.
- A student can enroll in multiple different course batches.
- Student photos must be optional.
- Do not permanently delete students with historical enrollments, attendance or payments.

---

## 12. Enrollment Management

An `Enrollment` connects one student to one course batch.

Example:

```text
Student: Nimal Perera
Course batch: 2026/PB02
Registration number: 2026/PB02/0001
Enrollment date: 2026-06-25
Status: ACTIVE
```

Supported enrollment statuses:

- `ACTIVE`
- `COMPLETED`
- `WITHDRAWN`
- `SUSPENDED`
- `CANCELLED`

Enrollment rules:

- A student cannot be enrolled in the same batch more than once.
- Registration number belongs to the enrollment, not the student.
- Registration number must be unique.
- Registration number format:

```text
{batchNumber}/{fourDigitSequence}
```

Example:

```text
2026/PB02/0001
```

- Registration numbers must be generated transactionally to avoid duplicates during concurrent enrollment.
- Do not generate registration numbers only in frontend code.
- Enrollment creation must also generate the enrollment's student charges from the batch fee plan.
- If charge generation fails, enrollment creation must roll back.

---

## 13. Lecturer Assignment

A batch may have one or multiple lecturers.

Store:

- Batch
- Lecturer
- Assignment start date
- Assignment end date, optional
- Status
- Audit fields

Rules:

- Lecturers can access only active assignments within authorized branches.
- Reassigning lecturers must not modify historical attendance ownership.
- Attendance records must keep the original user who marked or updated them.
- Administrators can assign or remove lecturers.
- Removing an assignment must not delete historical data.

---

## 14. Class Session Management

A `ClassSession` represents one real class held, scheduled, cancelled or rescheduled on a specific date.

Attendance must always be linked to a class session.

### Schedule modes

Each batch supports one of these modes:

#### `REGULAR`

The administrator defines one or more weekly schedules.

Example:

```text
Monday, 09:00-13:00
Wednesday, 09:00-13:00
```

The system must:

1. Generate sessions between the batch start and end dates.
2. Show a preview before saving generated sessions.
3. Avoid duplicate sessions.
4. Allow individual sessions to be edited afterward.
5. Allow additional one-off sessions.
6. Allow sessions to be cancelled or rescheduled.

#### `MANUAL`

The administrator or authorized lecturer creates sessions individually.

Use manual mode for irregular courses or schedules that are not confirmed in advance.

### Class session fields

- Batch
- Session date
- Start time
- End time
- Topic, optional
- Assigned lecturer
- Classroom or location, optional
- Status
- Cancellation reason, required when cancelled
- Original session reference, optional for rescheduling
- Remarks
- Audit fields

Supported statuses:

- `SCHEDULED`
- `COMPLETED`
- `CANCELLED`
- `RESCHEDULED`

Rules:

- Cancelled sessions do not count toward attendance percentage.
- A rescheduled class should preserve traceability to the original session.
- Do not permanently delete sessions that already have attendance records.
- Prevent duplicate active sessions for the same batch, date, start time and lecturer unless explicitly allowed by a future requirement.
- Attendance cannot be marked for a cancelled session.
- The system should warn before changing a session that already has attendance.

---

## 15. Attendance Management

Attendance is recorded per enrollment per class session.

Supported attendance statuses:

- `PRESENT`
- `ABSENT`
- `LATE`
- `EXCUSED`

Required attendance fields:

- Enrollment
- Class session
- Status
- Check-in time, optional
- Late minutes, optional
- Remarks, optional
- Marked by
- Marked date and time
- Last updated by
- Last updated date and time

Rules:

- Only one attendance record is allowed for one enrollment and one session.
- Enforce this with a database unique constraint.
- Attendance can be recorded only for an active or otherwise attendance-eligible enrollment.
- Attendance cannot be recorded for cancelled sessions.
- `LATE` may include check-in time and late minutes.
- `PRESENT`, `ABSENT` and `EXCUSED` do not require late minutes.
- Include a `Mark All Present` action.
- Require confirmation before overwriting already recorded attendance.
- Historical attendance edits must be audited.
- Cancelled sessions are excluded from attendance calculations.
- Excused attendance must be reported separately and should not automatically count as present unless a report explicitly defines that calculation.

Recommended default percentage calculation:

```text
attendancePercentage =
(PRESENT + LATE) / eligibleCompletedSessions * 100
```

`EXCUSED` is excluded from the numerator. Whether it is excluded from the denominator must be configurable in reporting logic and clearly stated in the report.

---

## 16. Fee Plan

Every course batch contains a fee plan with:

- Registration fee
- Total course fee
- Examination fee
- Duration in months
- Monthly installment due day
- Examination fee due date
- Currency code, default `LKR`
- Status
- Audit fields

Rules:

- Use `BigDecimal` in Java and `NUMERIC` in PostgreSQL.
- Never use floating-point types for money.
- Monthly due day is selected by an administrator when creating the batch.
- Examination fee due date is configurable per batch.
- Fees must not be negative.
- Duration months must be greater than zero.
- Total generated course installments must exactly equal the total course fee.

Default installment rule:

1. Divide the course fee by the duration in months.
2. Round according to the configured currency precision.
3. Add any remaining difference to the final installment.
4. Ensure the installment total equals the original course fee exactly.

If the selected due day does not exist in a month, use the final calendar day of that month.

---

## 17. Student Charges

When an enrollment is created, generate:

1. One registration fee charge
2. One course fee charge for each course month
3. One examination fee charge

Charge types:

- `REGISTRATION_FEE`
- `COURSE_INSTALLMENT`
- `EXAMINATION_FEE`
- `OTHER`, reserved for future use

Charge statuses:

- `UPCOMING`
- `DUE`
- `PAID`
- `OVERDUE`
- `WAIVED`
- `CANCELLED`

Rules:

- Partial payments are not allowed.
- A charge must be paid in full, waived in full, discounted to a final payable amount, or cancelled with proper authorization.
- A payment may settle one or more complete charges.
- The remaining payable amount of every selected charge must be fully allocated.
- A student charge must retain its original amount, discount amount, waiver amount and final payable amount.
- Charge status must be derived or updated consistently based on due date and payment state.
- Existing charges must not be recalculated when the batch fee plan changes.
- Overdue means the due date has passed and the final payable amount has not been fully settled or waived.

---

## 18. Payments

Supported payment methods:

- `CASH`
- `BANK_TRANSFER`
- `CARD`
- `CHEQUE`
- `OTHER`

Required payment fields:

- Enrollment
- Payment date
- Total amount
- Payment method
- Reference number, optional depending on method
- Remarks, optional
- Received by
- Receipt number
- Status
- Audit fields

Payment statuses:

- `COMPLETED`
- `VOIDED`

Rules:

- Partial charge settlement is not allowed.
- A payment can allocate money to multiple charges only when each selected charge is fully settled.
- The sum of allocations must equal the payment amount.
- Payment creation and charge allocation must occur in one database transaction.
- Completed payments must not be permanently deleted.
- Incorrect payments must be voided.
- Voiding requires a mandatory reason and authorized user.
- Voiding must reverse allocations and restore affected charge statuses.
- Every void action must be recorded in the audit log.
- Receipt numbers must be unique and generated on the server.
- Do not trust payment totals calculated only by the frontend.

---

## 19. Discounts and Waivers

Administrators may apply:

- Fixed amount discount
- Percentage discount
- Full fee waiver

Required fields:

- Student charge
- Adjustment type
- Original amount
- Discount or waiver amount
- Final payable amount
- Reason
- Approved by
- Created by
- Created date and time

Rules:

- A reason is mandatory.
- Approval information is mandatory.
- Lecturers cannot create, edit or remove adjustments.
- Adjustments must not make the final payable amount negative.
- Percentage discounts must have valid boundaries.
- Adjustments to already paid charges must be blocked unless a controlled reversal workflow is implemented.
- Changes must be auditable.
- Do not permanently delete applied adjustments.

---

## 20. Overdue Fee Warning During Attendance

Overdue fees must not block attendance.

When opening an attendance sheet, show a prominent warning next to affected students.

Lecturers may see:

- `Payment overdue`
- Total overdue amount

Lecturers must not see:

- Full payment history
- Individual receipt details
- Discounts
- Waivers
- Payment references
- Financial audit logs

Attendance must remain available even when a student has overdue fees.

---

## 21. Reports

### Attendance reports

Implement:

- Daily attendance sheet
- Attendance by batch
- Monthly attendance summary
- Individual student attendance
- Absent student report
- Late attendance report
- Attendance percentage report
- Sessions with incomplete attendance

### Fee reports

Implement:

- Student payment history
- Student outstanding balance
- Batch fee collection
- Monthly fee collection
- Overdue payment report
- Registration fee report
- Examination fee report
- Payment method summary
- Daily cashier collection
- Voided payment report
- Discounts and waivers report

Report filters should include relevant combinations of:

- Branch
- Course
- Batch
- Student
- Lecturer
- Date range
- Payment method
- Payment or charge status

Reports must support:

- On-screen viewing
- PDF export
- Excel export
- Printing

Do not load unbounded report datasets into memory. Use pagination or streaming where appropriate.

---

## 22. Dashboard Requirements

### Administrator dashboard

Show:

- Active students
- Active batches
- Today's classes
- Today's attendance percentage
- Students absent today
- Fees collected this month
- Total outstanding fees
- Total overdue fees
- Students with overdue payments
- Recent payments

### Lecturer dashboard

Show:

- Assigned batches
- Today's classes
- Upcoming sessions
- Sessions awaiting attendance
- Attendance percentage by assigned batch
- Frequently absent students

All dashboard data must respect branch and role permissions.

---

## 23. Backend Coding Standards

Use a feature-oriented package structure, for example:

```text
com.ihm.hotelschool
├── auth
├── branch
├── user
├── course
├── batch
├── student
├── enrollment
├── session
├── attendance
├── finance
├── report
├── audit
└── common
```

Rules:

- Do not expose JPA entities directly from controllers.
- Use request and response DTOs.
- Validate input with Bean Validation.
- Keep controllers thin.
- Put business rules in application or domain services.
- Use transactions for multi-step business operations.
- Use explicit database constraints.
- Use Flyway for every schema change.
- Never edit an already-applied production migration.
- Add a new migration instead.
- Use centralized exception handling.
- Return consistent error responses.
- Avoid N+1 queries.
- Add indexes for common searches and foreign keys.
- Use optimistic locking where concurrent updates are realistic.
- Store timestamps in UTC.
- Store class dates and fee due dates as date values when time-of-day is not required.
- Make the application timezone configurable, with `Asia/Colombo` as the initial default.

---

## 24. API Standards

Use REST APIs under:

```text
/api/v1
```

Guidelines:

- Use nouns for resources.
- Use correct HTTP methods.
- Use pagination for list endpoints.
- Support search and filtering through documented query parameters.
- Return `201 Created` for successful creation.
- Return `204 No Content` where appropriate for successful operations without a response body.
- Use consistent validation and authorization errors.
- Never expose stack traces or internal database details to clients.

Recommended error shape:

```json
{
  "timestamp": "2026-06-23T10:15:30Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [
    {
      "field": "nic",
      "message": "NIC is required"
    }
  ],
  "path": "/api/v1/students"
}
```

Use server-side filtering for branch, role and lecturer assignment. Never accept a branch ID from the client without verifying authorization.

---

## 25. Frontend Coding Standards

Use a feature-based structure, for example:

```text
src/app/
├── core/
├── shared/
├── auth/
├── dashboard/
├── branches/
├── users/
├── courses/
├── batches/
├── students/
├── enrollments/
├── sessions/
├── attendance/
├── finance/
└── reports/
```

Rules:

- Use strict TypeScript settings.
- Use typed interfaces or models.
- Use reactive forms.
- Display server-side validation messages clearly.
- Use route guards for navigation.
- Do not rely on route guards as the only security layer.
- Use HTTP interceptors for authentication and common error handling.
- Keep components focused.
- Move reusable logic into services or shared utilities.
- Show loading, success, empty and error states.
- Confirm destructive or irreversible actions.
- Use accessible labels, keyboard navigation and focus handling.
- Avoid hardcoded business values in templates.
- Display LKR amounts consistently.
- Display dates in an English Sri Lankan-friendly format.
- Do not expose detailed financial controls to lecturers.

---

## 26. Security Requirements

At minimum:

- Hash passwords securely.
- Never store plain-text passwords.
- Use short-lived access tokens and refresh tokens.
- Revoke refresh tokens when users are disabled or log out.
- Enforce password policies.
- Rate-limit login attempts.
- Validate file uploads.
- Restrict photo file types and sizes.
- Prevent insecure direct object references.
- Enforce branch and role authorization in backend services.
- Use HTTPS in deployed environments.
- Do not commit secrets.
- Provide `.env.example` with placeholder values only.
- Protect financial and personal data in logs.
- Audit sensitive actions.
- Keep dependencies updated through controlled upgrades.

---

## 27. Audit Requirements

Audit at least these actions:

- User creation, update, disable and role changes
- Branch assignment changes
- Course and batch changes
- Student creation and important profile updates
- Enrollment creation, status changes and cancellation
- Lecturer assignment changes
- Session cancellation and rescheduling
- Attendance edits after initial submission
- Fee plan changes
- Discount and waiver creation or modification
- Payment creation
- Payment voiding
- Receipt regeneration
- Financial report exports where practical

Audit records should include:

- Acting user
- Action
- Entity type
- Entity ID
- Timestamp
- Branch
- Previous value where appropriate
- New value where appropriate
- Reason where required

Audit logs must not be editable through normal application workflows.

---

## 28. Testing Requirements

### Backend

Add tests for:

- NIC uniqueness
- Duplicate batch number prevention
- Duplicate enrollment prevention
- Transaction-safe registration number generation
- Automatic session generation
- Duplicate session prevention
- Cancelled session attendance blocking
- One attendance record per enrollment and session
- Lecturer assignment authorization
- Branch authorization
- Fee installment generation
- Final installment rounding
- Invalid due day handling
- No partial payment enforcement
- Multi-charge full payment allocation
- Payment amount and allocation equality
- Payment voiding
- Discount and waiver validation
- Overdue status calculation
- Overdue warning without attendance blocking

Use:

- Unit tests for business logic
- Integration tests for repositories and transactions
- Security tests for role and branch restrictions
- Testcontainers for PostgreSQL integration testing where practical

### Frontend

Add tests for:

- Form validation
- Role-based control visibility
- Attendance marking workflow
- Mark-all-present behaviour
- Overdue warning display
- No partial-payment UI flow
- Session generation preview
- Report filter behaviour
- API error handling

### End-to-End

Cover critical flows:

1. Admin creates branch, course and batch.
2. Admin configures the batch fee plan and schedule.
3. Admin creates or finds a student by NIC.
4. Admin enrolls the student.
5. Registration number and charges are generated.
6. Lecturer sees the assigned batch.
7. Lecturer marks attendance.
8. Admin records a full-charge payment.
9. Receipt is generated.
10. Overdue warning appears when an unpaid charge passes its due date.
11. Attendance remains possible despite overdue fees.

Do not declare a feature complete while important tests are failing.

---

## 29. Database Requirements

Use:

- Primary keys
- Foreign keys
- Unique constraints
- Check constraints where supported
- Audit columns
- Useful indexes

Important unique constraints include:

- Branch code
- Course short code
- Batch number
- Normalized NIC
- Registration number
- Student plus batch enrollment
- Enrollment plus class session attendance
- Receipt number

Recommended money columns:

```sql
NUMERIC(14, 2)
```

Avoid database-specific logic that makes future migration unnecessarily difficult, unless PostgreSQL provides a clear reliability benefit.

---

## 30. Data Integrity and Deletion Policy

Do not hard-delete records that have business history.

Prefer:

- Status changes
- Soft deletion where appropriate
- Voiding for payments
- Cancellation for sessions, enrollments and charges

Hard deletion may be allowed only for clearly unused draft records with no dependent data and only through an authorized workflow.

Financial and attendance history must remain traceable.

---

## 31. Performance Requirements

- Paginate student, enrollment, payment and report lists.
- Add search indexes for NIC, registration number, name and batch number.
- Avoid loading student photos in full-size list views.
- Use thumbnails for lists.
- Avoid N+1 queries.
- Use database aggregation for dashboards and reports.
- Do not calculate large reports entirely in the browser.
- Keep common attendance screens fast on mobile connections.

---

## 32. Development Commands

Agents must prefer repository-provided wrappers and scripts.

Expected backend commands:

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

Expected frontend commands:

```bash
cd frontend
npm install
npm start
npm test
npm run build
```

Expected local infrastructure command:

```bash
docker compose up -d
```

If the actual repository uses different commands, update this section and the README.

---

## 33. Implementation Order

Implement in this order unless a task explicitly requires otherwise:

1. Repository setup and local infrastructure
2. Authentication and authorization
3. Branch management
4. User and lecturer management
5. Course management
6. Course batch management
7. Student management
8. Enrollment and registration-number generation
9. Lecturer assignment
10. Batch schedules and class sessions
11. Attendance
12. Fee plan and automatic charge generation
13. Discounts and waivers
14. Payments, allocations, voiding and receipts
15. Dashboards
16. Reports and exports
17. Audit views
18. Security review
19. End-to-end testing
20. Deployment documentation

Do not start advanced dashboards or reporting before the underlying transactional workflows are stable.

---

## 34. Definition of Done

A task is complete only when:

- The requested behaviour is implemented.
- Authorization is enforced in the backend.
- Input validation is implemented.
- Database constraints are added where required.
- Migrations are included.
- Automated tests cover important logic.
- Existing tests pass.
- The frontend has loading, empty, success and error states.
- No secrets are committed.
- Documentation is updated.
- The application builds successfully.
- The agent reports assumptions and remaining limitations.

---

## 35. Non-Negotiable Business Rules

These rules must not be changed without explicit approval:

1. NIC is mandatory and unique.
2. A student may enroll in multiple batches.
3. A student may not enroll in the same batch twice.
4. Registration number belongs to the enrollment.
5. Registration numbers are generated on the server.
6. Every batch belongs to one branch.
7. Lecturers see only assigned batches.
8. Attendance is recorded against class sessions.
9. Sessions may be generated from a regular schedule or created manually.
10. Cancelled sessions do not count toward attendance.
11. One attendance record is allowed per enrollment per session.
12. Batch fees include registration, course and examination fees.
13. Course fees are split across the batch duration in months.
14. Monthly installment due day is selected by an administrator.
15. Examination fee due date is configurable per batch.
16. Partial payments are not accepted.
17. A payment must fully settle every selected charge.
18. Discounts and waivers require a reason and approval.
19. Overdue fees show a warning but do not block attendance.
20. Lecturers cannot manage financial records.
21. Completed payments cannot be deleted; they may only be voided.
22. Sensitive changes must be audited.
23. Existing enrollment charges must not change when future batch fees change.
24. The system must support multiple branches even though only one branch exists initially.
25. The application is English only.

---

## 36. Instructions for Future Agents

When a requested change conflicts with this file:

1. Stop before implementing the conflicting change.
2. Identify the exact rule that conflicts.
3. Explain the impact.
4. Ask for explicit approval.
5. Update this file and related documentation only after approval.

Do not silently reinterpret financial, attendance, authorization or audit rules.

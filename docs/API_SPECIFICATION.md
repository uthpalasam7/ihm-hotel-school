# API Specification

Planning update, 20 September 2026: student-card and delivery operations (Phase 6),
session-scoped QR attendance (Phase 8), and receipt delivery (Phase 10) are approved
future scope, not currently available endpoints. See the
[card, scanning, and delivery plan](STUDENT_CARDS_QR_AND_DELIVERY_PLAN.md).

Define their exact request/response contracts during the corresponding phase:
authenticated card issuance/preview/PDF/revocation/replacement; optional card and
receipt email requests with delivery-status/retry access; and duplicate-safe scans
bound to a session. Keep QR tokens out of URLs and public lookup routes. Apply the
same server-side role/branch/assignment checks as the underlying resources, return
only permitted scan details, and distinguish queued delivery from successful
delivery. This planning update does not claim these routes are implemented.

## 1. General Standards

Base path:

```text
/api/v1
```

Content type:

```text
application/json
```

Use:

- JWT bearer authentication
- Pagination
- Server-side filtering
- Consistent errors
- Branch-aware authorization
- `X-Active-Branch-Id` on branch-scoped requests when the UI has an active branch selected
- ISO-8601 dates and timestamps
- Decimal JSON values for money

The active branch header is only a request context. The backend must validate that the authenticated user can access the branch and must still apply branch predicates to branch-owned data. If both `X-Active-Branch-Id` and an explicit `branchId` query parameter are present, the endpoint-specific query parameter is the explicit filter and remains subject to authorization.

## 2. Standard Pagination

Request:

```text
?page=0&size=20&sort=createdAt,desc
```

Response:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## 3. Standard Error

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
  "path": "/api/v1/students",
  "requestId": "..."
}
```

## 4. Authentication

### `POST /auth/login`

Request:

```json
{
  "username": "admin",
  "password": "secret"
}
```

Response:

```json
{
  "accessToken": "...",
  "accessTokenExpiresAt": "...",
  "refreshToken": "...",
  "user": {
    "id": 1,
    "fullName": "Administrator",
    "roles": ["ADMIN"],
    "branches": [
      {
        "id": 1,
        "code": "IHM-MAIN",
        "name": "IHM Hotel School"
      }
    ]
  }
}
```

### `POST /auth/refresh`
### `POST /auth/logout`
### `GET /auth/me`
### `POST /auth/change-password`

## 5. Branches

### `GET /branches`
### `POST /branches`
### `GET /branches/{id}`
### `PUT /branches/{id}`
### `PATCH /branches/{id}/status`

Only `SUPER_ADMIN` may create, update or deactivate branches. Branch-scoped users may list or read only assigned branches where an endpoint needs branch choices.

## 6. Users

### `GET /users`

Filters:

- role
- branchId
- status
- search

### `POST /users`
### `GET /users/{id}`
### `PUT /users/{id}`
### `PATCH /users/{id}/status`
### `PUT /users/{id}/roles`
### `PUT /users/{id}/branches`
### `POST /users/{id}/reset-password`

Do not return password hashes.

For Phase 3, `SUPER_ADMIN` may manage all user roles and branch assignments. `ADMIN` may create and manage only `LECTURER` accounts within the admin's assigned branches. Disabling a user and resetting a password revoke active refresh tokens.

## 7. Courses

### `GET /courses`

Filters:

- status
- search

### `POST /courses`

Request:

```json
{
  "name": "Pastry & Bakery",
  "shortCode": "PB",
  "description": "Certificate course in pastry and bakery",
  "status": "ACTIVE"
}
```

### `GET /courses/{id}`
### `PUT /courses/{id}`
### `PATCH /courses/{id}/status`

Course short codes are normalized to uppercase and must remain unique. Course management is limited to `SUPER_ADMIN` and `ADMIN`.

## 8. Course Batches

### `GET /batches`

Filters:

- branchId
- courseId
- lecturerId
- status
- startDateFrom
- startDateTo
- search

### `POST /batches`

Request:

```json
{
  "courseId": 1,
  "branchId": 1,
  "batchNumber": "2026/PB02",
  "startDate": "2026-07-01",
  "endDate": "2026-12-31",
  "durationMonths": 6,
  "scheduleMode": "REGULAR",
  "status": "UPCOMING",
  "remarks": null
}
```

### `GET /batches/{id}`
### `PUT /batches/{id}`
### `PATCH /batches/{id}/status`

Batch reads and writes are branch-aware. `SUPER_ADMIN` may access all branches; `ADMIN` may manage only assigned branches; `LECTURER` may read only batches with active lecturer assignments in authorized branches. Batch mutations require `SUPER_ADMIN` or `ADMIN`.

## 9. Batch Lecturer Assignments

### `GET /batches/{batchId}/lecturers`
### `POST /batches/{batchId}/lecturers`
### `PUT /batches/{batchId}/lecturers`
### `PUT /batches/{batchId}/lecturers/{assignmentId}`
### `PATCH /batches/{batchId}/lecturers/{assignmentId}/status`

Request:

```json
{
  "lecturerUserId": 20,
  "assignmentStartDate": "2026-07-01",
  "assignmentEndDate": null,
  "status": "ACTIVE"
}
```

The batch form uses `PUT /batches/{batchId}/lecturers` to synchronize the selected active lecturer set.
The backend deduplicates `lecturerUserIds`, keeps already selected active assignments, creates only new active assignments, and marks unselected active assignments inactive for history.

Request:

```json
{
  "lecturerUserIds": [20, 21],
  "assignmentStartDate": "2026-07-01",
  "assignmentEndDate": null
}
```

Selected users must exist, be active, have the `LECTURER` role, and be assigned to the batch branch.

## 10. Students

### `GET /students`

Filters:

- status
- nic
- search

Phase 5 treats Student as a global identity directory. `search` matches name,
normalized NIC, primary contact number, and alternative contact number.
Enrollment-derived `branchId`, `batchId`, and `registrationNumber` filters are
deferred until Phase 6.

### `POST /students`

Request:

```json
{
  "fullName": "Nimal Perera",
  "nic": "200012345678",
  "contactNumber": "0712345678",
  "alternativeContactNumber": null,
  "email": null,
  "address": "Kurunegala",
  "dateOfBirth": null,
  "gender": null,
  "remarks": null
}
```

New students default to `ACTIVE`. Both `SUPER_ADMIN` and `ADMIN` may manage
students. Lecturers cannot access Student endpoints until enrollment scope is
available. `nic` accepts numbers and letters only and is stored in uppercase.
`contactNumber` must be exactly 10 digits. `alternativeContactNumber` is optional
but must be exactly 10 digits when provided. `gender` is optional and must be one
of `Male`, `Female`, or `Other` when provided.

Response:

```json
{
  "id": 100,
  "fullName": "Nimal Perera",
  "nic": "200012345678",
  "contactNumber": "0712345678",
  "alternativeContactNumber": null,
  "email": null,
  "address": "Kurunegala",
  "dateOfBirth": null,
  "gender": null,
  "remarks": null,
  "status": "ACTIVE",
  "photoAvailable": false,
  "photoUrl": null,
  "photoThumbnailUrl": null,
  "createdAt": "2026-07-21T01:00:00Z",
  "updatedAt": "2026-07-21T01:00:00Z",
  "version": 0
}
```

### `GET /students/{id}`
### `PUT /students/{id}`
### `PATCH /students/{id}/status`
### `GET /students/by-nic/{nic}`
### `POST /students/{id}/photo`
### `GET /students/{id}/photo`
### `DELETE /students/{id}/photo`

Photo upload uses multipart field `photo`. JPEG and PNG are accepted up to
5 MiB after MIME and image-signature validation. Images are re-encoded, the
full variant is bounded to 1600 pixels, and a 96-pixel list thumbnail is
available from `GET /students/{id}/photo?variant=thumbnail`.

Photo responses are authenticated and served with private, no-store and
no-sniff headers. Photo deletion removes only the current photo, not the
student. There is no Student deletion endpoint; status changes preserve the
record.

## 11. Enrollments

Implemented in the first Phase 6 delivery. Admin and Super Administrator only;
branch authorization is enforced on reads, previews, and creation. All paths
below are relative to `/api/v1` and support the active branch header.

### `GET /enrollments`

Paged; filters: `branchId`, `batchId`, `studentId`, `status`, and `search`
(registration number, student name, or batch number). Results are always
limited to authorized branches.

### `POST /enrollments/preview`

Read-only preview. Requires `studentId`, `batchId`, and `enrollmentDate` in the
same shape as creation. Returns `currencyCode`, `totalAmount`, `batchVersion`,
`feePlanVersion`, and a `charges` array with type, installment number,
description, due date, and amount. No enrollment or charges are saved.

### `POST /enrollments`

```json
{
  "studentId": 100,
  "batchId": 10,
  "enrollmentDate": "2026-06-25",
  "remarks": null,
  "expectedBatchVersion": 3,
  "expectedFeePlanVersion": 2
}
```

The two expected versions are optional API fields and are sent by the UI after
preview. A changed batch or fee plan causes `409 Conflict`, so staff can review
fresh charges. Duplicate student and batch, or an exhausted four-digit sequence,
also returns `409`. A successful request returns `201 Created` with `id`,
student and batch summaries, server-generated `registrationNumber`,
`enrollmentDate`, `status`, `remarks`, `createdAt`, and `version`. Generated
charges are read separately. Enrollment, charges, sequence, and audit record
commit together.

### `GET /enrollments/{id}`
### `GET /students/{studentId}/enrollments`
### `GET /batches/{batchId}/enrollments`
### `GET /enrollments/{id}/charges`

Charge lists are paged. Each row includes original, discount, waiver, and final
payable amounts; currency; due date; and an effective due status for today.

### `PATCH /enrollments/{id}/status`

Implemented for administrators with branch access. Request fields are `status`
(`ACTIVE`, `SUSPENDED`, `COMPLETED`, `WITHDRAWN`, or `CANCELLED`), a required
`reason`, and optional optimistic-lock `version`. Active enrollments may be
suspended, completed, withdrawn, or cancelled; suspended enrollments may be
resumed, withdrawn, or cancelled. Terminal states cannot be reopened. Each
change is audited. Existing charges and student cards remain unchanged and
require their own authorized workflows.

### `GET /batches/{batchId}/students`

Paged minimal roster for administrators with branch access and lecturers with
a current active assignment in the batch and its branch. Rows include student
name, registration number, enrollment date, and enrollment status. The response
does not include NIC, contact details, enrollment remarks, or fee information.
Lecturers cannot access general enrollment or charge endpoints.

### Student card and card email — implemented Phase 6 endpoints

All card operations require an administrator with access to a branch in which
the student is enrolled. The active branch header, when supplied, must match one
of those enrollments. No raw QR token is returned as JSON.

- `GET /students/{studentId}/card`: card metadata, or `204` when not issued.
- `POST /students/{studentId}/card`: issue, or return the active card without
  creating a second credential.
- `POST /students/{studentId}/card/replace`: `{ "reason": "Lost card" }`.
- `POST /students/{studentId}/card/revoke`: `{ "reason": "Returned card" }`.
- `GET /students/{studentId}/card/history`: paged issuance/replacement/revocation.
- `GET /students/{studentId}/card/qr`: protected `image/png`, active card only.
- `GET /students/{studentId}/card/pdf`: protected two-page wallet-size PDF,
  active card only. Both document responses use `Cache-Control: no-store`.
- `GET /document-deliveries/availability`: whether SMTP card email is configured.
- `POST /students/{studentId}/card/email`: body contains a client-generated UUID
  `idempotencyKey`. Returns `202` with queued delivery status. Repeating the same
  key returns the existing request; a deliberate resend uses a new key.
- `GET /students/{studentId}/card/deliveries`: paged delivery history with the
  saved recipient snapshot, attempts, status, and provider-acceptance timestamp.

Email sends only to the student's saved email. Missing address yields `400`;
unconfigured SMTP yields `503`. A replaced or revoked card queued earlier will
not be sent. `ACCEPTED` means accepted by the SMTP service, not confirmed in the
student's inbox.

## 12. Batch Schedules

### `GET /batches/{batchId}/schedules`
### `POST /batches/{batchId}/schedules`
### `PUT /batches/{batchId}/schedules/{scheduleId}`
### `DELETE /batches/{batchId}/schedules/{scheduleId}`

Deletion is allowed only when no dependent generated history requires preservation.

## 13. Session Generation

### `POST /batches/{batchId}/sessions/preview`

Request:

```json
{
  "fromDate": "2026-07-01",
  "toDate": "2026-12-31",
  "excludeDates": ["2026-08-01"]
}
```

Response includes proposed dates, duplicate warnings, and validation errors.

### `POST /batches/{batchId}/sessions/generate`

Uses a confirmed preview token or matching request details to generate sessions transactionally.

## 14. Class Sessions

### `GET /sessions`

Filters:

- branchId
- batchId
- lecturerId
- dateFrom
- dateTo
- status
- attendanceState

### `POST /sessions`
### `GET /sessions/{id}`
### `PUT /sessions/{id}`
### `POST /sessions/{id}/cancel`

Request:

```json
{
  "reason": "Public holiday"
}
```

### `POST /sessions/{id}/reschedule`

Request:

```json
{
  "newDate": "2026-07-23",
  "newStartTime": "09:00",
  "newEndTime": "13:00",
  "lecturerUserId": 20,
  "reason": "Lecturer unavailable"
}
```

## 15. Attendance

### `GET /sessions/{sessionId}/attendance-sheet`

Response:

```json
{
  "session": {},
  "students": [
    {
      "enrollmentId": 500,
      "registrationNumber": "2026/PB02/0001",
      "studentName": "Nimal Perera",
      "photoUrl": null,
      "attendance": null,
      "paymentWarning": {
        "overdue": true,
        "overdueAmount": 10000.00
      }
    }
  ]
}
```

For lecturers, no detailed financial data is returned.

### `PUT /sessions/{sessionId}/attendance`

Request:

```json
{
  "records": [
    {
      "enrollmentId": 500,
      "status": "PRESENT",
      "checkInTime": null,
      "lateMinutes": null,
      "remarks": null
    }
  ]
}
```

### `POST /sessions/{sessionId}/attendance/mark-all-present`
### `POST /sessions/{sessionId}/attendance/submit`
### `GET /students/{studentId}/attendance`
### `GET /enrollments/{enrollmentId}/attendance-summary`

Bulk attendance operations must be transactional.

## 16. Fee Plans

### `GET /batches/{batchId}/fee-plan`
### `POST /batches/{batchId}/fee-plan`
### `PUT /batches/{batchId}/fee-plan`
### `POST /batches/{batchId}/fee-plan/installment-preview`

Request:

```json
{
  "registrationFee": 5000.00,
  "courseFee": 60000.00,
  "examinationFee": 7500.00,
  "durationMonths": 6,
  "monthlyDueDay": 10,
  "examinationDueDate": "2026-11-15",
  "currencyCode": "LKR",
  "status": "ACTIVE"
}
```

Preview response:

```json
{
  "currencyCode": "LKR",
  "totalAmount": 72500.00,
  "charges": [
    {
      "type": "REGISTRATION_FEE",
      "installmentNumber": null,
      "description": "Registration fee",
      "dueDate": "2026-07-01",
      "amount": 5000.00
    }
  ]
}
```

The installment preview is display-only and does not create student charges. Changing the plan must not update already generated enrollment charges once enrollments exist.

## 17. Student Charges

### `GET /enrollments/{enrollmentId}/charges`
### `GET /charges/{id}`
### `POST /charges/{id}/discount`
### `POST /charges/{id}/waive`
### `POST /charges/{id}/cancel`

Discount request:

```json
{
  "type": "FIXED",
  "amount": 1000.00,
  "reason": "Approved promotional discount",
  "approvedByUserId": 1
}
```

Waiver request:

```json
{
  "reason": "Approved scholarship",
  "approvedByUserId": 1
}
```

## 18. Payments

### `GET /payments`

Filters:

- branchId
- batchId
- enrollmentId
- paymentDateFrom
- paymentDateTo
- method
- status
- receiptNumber

### `POST /payments`

Request:

```json
{
  "enrollmentId": 500,
  "paymentDate": "2026-07-10",
  "paymentMethod": "CASH",
  "referenceNumber": null,
  "remarks": null,
  "chargeIds": [1001, 1002]
}
```

The backend calculates the exact payment amount from the selected charges.

Response:

```json
{
  "paymentId": 700,
  "totalAmount": 15000.00,
  "status": "COMPLETED",
  "receiptNumber": "IHM-MAIN-2026-000001"
}
```

### `GET /payments/{id}`
### `POST /payments/{id}/void`

Request:

```json
{
  "reason": "Payment entered against the wrong student"
}
```

### `GET /payments/{id}/receipt`
### `POST /payments/{id}/receipt/regenerate`

## 19. Dashboards

### `GET /dashboard/admin`
### `GET /dashboard/lecturer`

Filters include branch and date where authorized.

## 20. Reports

Attendance:

- `GET /reports/attendance/daily`
- `GET /reports/attendance/batch`
- `GET /reports/attendance/monthly`
- `GET /reports/attendance/student`
- `GET /reports/attendance/absent`
- `GET /reports/attendance/late`
- `GET /reports/attendance/percentage`
- `GET /reports/attendance/incomplete-sessions`

Finance:

- `GET /reports/finance/student-history`
- `GET /reports/finance/outstanding`
- `GET /reports/finance/batch-collection`
- `GET /reports/finance/monthly-collection`
- `GET /reports/finance/overdue`
- `GET /reports/finance/registration-fees`
- `GET /reports/finance/examination-fees`
- `GET /reports/finance/payment-methods`
- `GET /reports/finance/daily-cashier`
- `GET /reports/finance/voided-payments`
- `GET /reports/finance/adjustments`

Export query:

```text
?format=pdf
?format=xlsx
```

Exports must enforce the same authorization as screen reports.

## 21. Audit

### `GET /audit-logs`

Super administrator or permitted administrator only.

Filters:

- branchId
- actorUserId
- entityType
- entityId
- action
- dateFrom
- dateTo

Audit logs are read-only.

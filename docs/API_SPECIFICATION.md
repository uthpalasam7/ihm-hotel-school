# API Specification

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

- branchId through enrollment
- batchId
- status
- nic
- registrationNumber
- search

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

### `GET /students/{id}`
### `PUT /students/{id}`
### `PATCH /students/{id}/status`
### `GET /students/by-nic/{nic}`
### `POST /students/{id}/photo`
### `GET /students/{id}/photo`
### `DELETE /students/{id}/photo`

Photo deletion removes only the current photo, not the student.

## 11. Enrollments

### `GET /enrollments`

Filters:

- branchId
- batchId
- studentId
- status
- registrationNumber
- search

### `POST /enrollments`

Request:

```json
{
  "studentId": 100,
  "batchId": 10,
  "enrollmentDate": "2026-06-25",
  "remarks": null
}
```

Response:

```json
{
  "id": 500,
  "registrationNumber": "2026/PB02/0001",
  "status": "ACTIVE",
  "generatedCharges": [
    {
      "id": 1,
      "type": "REGISTRATION_FEE",
      "dueDate": "2026-06-25",
      "amount": 5000.00
    }
  ]
}
```

### `GET /enrollments/{id}`
### `PATCH /enrollments/{id}/status`
### `GET /students/{studentId}/enrollments`
### `GET /batches/{batchId}/enrollments`

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

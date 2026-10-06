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

`GET` returns the full assignment history to branch-authorized administrators.
A lecturer may read only ACTIVE assignment rows for a batch where that lecturer
has a current ACTIVE assignment in an authorized branch. An expired, inactive,
future, or unassigned lecturer receives 403. This read supports session lecturer
choices; all assignment write endpoints remain administrator-only.

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

Implemented in Phase 7.2. All paths below are relative to `/api/v1`.
SUPER_ADMIN and branch-authorized ADMIN manage patterns and generate sessions.
LECTURER can read patterns only for currently assigned batches in authorized
branches; regular-pattern mutation and bulk generation are administrator workflows.
The optional active-branch header is validated, and authorization always checks
the actual batch branch. Active/upcoming REGULAR batches in active branches are
required for creation, update, preview and generation.

### `GET /batches/{batchId}/schedules`

Paginated `PageResponse<ScheduleResponse>`, including inactive history, ordered
by ISO weekday, start time and ID. `page=0`, `size=20` by default, size capped at
100. Response fields: `id`, `batchId`, `dayOfWeek`, `startTime`, `endTime`,
`defaultLecturerUserId`, `defaultLecturerName`, `classroom`, `status`, `version`.

### `POST /batches/{batchId}/schedules`

Returns 201 with the saved pattern. Example:

```json
{
  "dayOfWeek": 1,
  "startTime": "09:00",
  "endTime": "13:00",
  "defaultLecturerUserId": 20,
  "classroom": "Kitchen 1",
  "status": "ACTIVE"
}
```

ISO weekday is 1 (Monday) through 7 (Sunday). End must be after start on the same
day. Lecturer and classroom are optional; classroom is limited to 150 characters.
An active pattern's named lecturer must be an active lecturer user assigned to
the branch and have an active batch assignment overlapping the batch dates.
Generation additionally checks assignment coverage for each proposed date.
Active patterns cannot overlap in a batch, regardless of lecturer; adjacent
intervals are allowed. At most 100 active patterns are supported per batch.

### `PUT /batches/{batchId}/schedules/{scheduleId}`

Same body as creation, with required `version` from the saved response.
Missing/stale version returns 409. Edits affect subsequent generation only;
already saved sessions retain their dates, times, lecturer and classroom.

### `DELETE /batches/{batchId}/schedules/{scheduleId}?version=0`

Returns 204 and **deactivates** the pattern. It does not hard-delete the pattern
or any generated sessions. Version is required; stale versions return 409.
Deactivation is also available for retired batches. Create/update/deactivate
record audit events. Switching a batch to MANUAL requires deactivating its active
weekly patterns first.

## 13. Session Generation

Implemented in Phase 7.2. Generation and pattern/batch/assignment changes acquire
the same batch row lock. Request bodies use date-only `YYYY-MM-DD` values; class
times remain local wall-clock values, independent of UTC audit timestamps.

### `POST /batches/{batchId}/sessions/preview`

```json
{
  "fromDate": "2026-07-01",
  "toDate": "2026-07-31",
  "excludeDates": ["2026-07-08"]
}
```

The inclusive range must be inside the batch dates and span at most 366 days.
Exclusions are optional, must fall inside the range, and are deduplicated.
At most 1000 proposed sessions, 5000 existing session-context records and 1000
active lecturer assignments are examined per request; exceeding a bound returns
400 with a message. Narrow the range where applicable. At least one active
pattern is required. No matching dates (or excluding all dates) returns an empty
preview and can be confirmed as a no-op.

Returns `previewToken`, `expiresAt`, `createCount`, `skipCount`, `conflictCount`,
and `sessions`. Each entry includes `scheduleId`, `sessionDate`, `startTime`,
`endTime`, `lecturerUserId`, `lecturerName`, `classroom`, `outcome`,
`existingSessionId`, and an optional explanation in `message`.

Entry outcomes:

- `CREATE`: a valid new session.
- `ALREADY_GENERATED`: the pattern/date already generated a session; preserve it
  even if it was edited, cancelled or rescheduled later.
- `EXISTING`: a matching session already exists. A cancelled/rescheduled session
  with the same date/start/lecturer is retained too, rather than resurrected.
- `CONFLICT`: a different active session overlaps this batch's proposed interval,
  or the named lecturer lacks active branch/batch eligibility for that date.

Preview changes no session records. The 30-minute HMAC confirmation token uses
an application-specific signing context with the configured JWT secret and is
bound to actor, batch/version, range, normalized exclusions and active pattern
contents/versions. It is not a login token. Do not place it in URLs.

### `POST /batches/{batchId}/sessions/generate`

Send the same range/exclusions plus `previewToken` from the preview:

```json
{
  "fromDate": "2026-07-01",
  "toDate": "2026-07-31",
  "excludeDates": ["2026-07-08"],
  "previewToken": "<token returned by preview>"
}
```

Returns 200, for example:

```json
{
  "createdCount": 4,
  "skippedCount": 0,
  "createdSessionIds": [101, 102, 103, 104]
}
```

Missing, expired, tampered, different-actor or stale-plan tokens return 409.
The server recalculates duplicates/conflicts under the batch lock. A concurrent
successful generation can change CREATE entries into skips without invalidating
the confirmation; any new conflict aborts the entire operation. All newly created
sessions and the generation audit event commit in one transaction. Failure rolls
back all new sessions. Reusing a still-valid confirmation creates no duplicates;
a pure no-op does not add another generation audit event. After expiry, obtain a
fresh preview before retrying.

Generated sessions persist their source pattern and original generation date.
This identity survives later edits or cancellation/rescheduling. Duplicate active
session keys also have a database unique index, including the unassigned-lecturer
case. Saved sessions prevent later batch identity changes or date ranges that
exclude those sessions. Bulk generation never records attendance.

## 14. Class Sessions

### `GET /sessions`

List/detail were implemented in Phase 7.1. Creation and editing are implemented
in Phase 7.3; cancellation and rescheduling are implemented in Phase 7.4.

Filters: `branchId`, `batchId`, `lecturerId`, `dateFrom`, `dateTo`, `status`.
Dates use `YYYY-MM-DD`, both bounds are inclusive, and an inverted range returns
400. Status accepts SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED (case-insensitive).
IDs must be positive. `page` defaults to 0 and must be non-negative; `size`
defaults to 20, must be positive, and is capped at 100. Ordering is fixed by
session date, start time, then ID. Returns the shared paginated `PageResponse`.
`attendanceState` filtering is deferred to Phase 8.

Without `branchId`, the validated `X-Active-Branch-Id` header is the default
filter. Explicit filters never widen the caller's authorized scope. SUPER_ADMIN
can read all branches; ADMIN reads assigned branches; LECTURER additionally needs
an ACTIVE batch assignment covering today in the configured application timezone.
Lecturers can read all sessions in such a batch, including sessions led by a
co-lecturer or with no named lecturer. Being named on a session alone grants no
access. The same rules apply to direct detail URLs. Expired/future/inactive
assignments grant no access. Historical sessions remain readable while the caller
has current batch access; session date does not replace the current-access check.

List counts are scoped in the database. Unauthorized explicit branch/batch filters
and detail access return 403; missing batch/detail records return 404.

Example detail response (illustrative IDs):

```json
{
  "id": 12,
  "batchId": 7,
  "batchNumber": "2026/CK01",
  "courseName": "Professional Cookery",
  "branchId": 1,
  "branchName": "IHM Hotel School",
  "sessionDate": "2026-10-05",
  "startTime": "09:00:00",
  "endTime": "13:00:00",
  "lecturerUserId": 20,
  "lecturerName": "Example Lecturer",
  "topic": "Kitchen safety",
  "classroom": "Kitchen 1",
  "status": "SCHEDULED",
  "cancellationReason": null,
  "originalSessionId": null,
  "remarks": null,
  "attendanceSubmittedAt": null,
  "createdAt": "2026-10-04T02:00:00Z",
  "updatedAt": "2026-10-04T02:00:00Z",
  "version": 0,
  "sourceScheduleId": null,
  "generationDate": null,
  "reschedulingReason": null
}
```

Responses contain no student personal data or financial details. Local class
date/time values are interpreted in the application timezone; audit timestamps
are UTC. The Phase 7.6 admin screen uses these existing endpoints without
changing their contracts.

### `POST /sessions`
### `PUT /sessions/{id}`

Implemented in Phase 7.3. POST returns 201 with `SessionResponse`; PUT returns
200 with the updated response. Both accept:

```json
{
  "batchId": 7,
  "sessionDate": "2026-10-05",
  "startTime": "09:00",
  "endTime": "13:00",
  "lecturerUserId": 20,
  "topic": "Kitchen safety",
  "classroom": "Kitchen 1",
  "remarks": "Bring safety shoes",
  "version": 0
}
```

`batchId`, date and both times are required. `version` is required for PUT and
must match the latest response; omit it for POST. PUT replaces the editable
fields, so omitted optional fields become null. IDs must be positive. Topic,
classroom and remarks have limits of 300, 150 and 2000 characters, are trimmed,
and blank values become null. Lecturer is optional; when supplied, the user must
be an active LECTURER in the batch branch with an ACTIVE assignment covering the
proposed session date (inclusive).

SUPER_ADMIN and branch-authorized ADMIN can write. LECTURER additionally needs a
current ACTIVE batch assignment covering today, using the same batch access rules
as the read endpoints. This permits managing co-lecturer/unassigned sessions in
that batch; being named on a session alone grants no access. A named lecturer's
session-date eligibility is separate from the caller's current access check.
The active-branch header is validated and never grants additional permissions.

Both MANUAL and REGULAR batches allow one-off sessions. The branch must be ACTIVE
and the batch ACTIVE or UPCOMING. Dates must be within inclusive batch bounds,
and start must precede end on the same day. Overlaps with SCHEDULED or COMPLETED
sessions in the same batch return 409, regardless of lecturer. Adjacent intervals
are allowed. CANCELLED and RESCHEDULED records do not occupy an active slot.
Cross-batch classroom/lecturer resource conflicts are not enforced in this phase.

New sessions are SCHEDULED. Batch cannot change on edit. PUT only edits SCHEDULED
sessions without submitted attendance; terminal states or submitted attendance
return 409. Missing/stale versions also return 409. Date/time correction preserves
the same session ID; the separate lifecycle rescheduling operation in 7.4
preserves an original/replacement link. Generated sessions retain their original
`sourceScheduleId` and `generationDate`, so generation cannot recreate their old
slot after an edit. Status and provenance are controlled by the server.

Writes lock the batch, recheck validation, and save session plus SESSION_CREATED
or SESSION_UPDATED audit evidence in one transaction. Edits record old/new values.
Bulk generation uses the same lock. Invalid input returns 400, unauthorized access
403, and missing batch/session/lecturer 404. No hard-delete endpoint is provided.

Attendance records do not exist yet: Phase 8 must extend the edit guard to detect
any saved attendance, including drafts, and implement the confirmed/audited
correction flow before allowing changes to such sessions.

### `POST /sessions/{id}/cancel`

Implemented in Phase 7.4. Returns 200 with the updated `SessionResponse`.

```json
{
  "reason": "Public holiday",
  "version": 0
}
```

### `POST /sessions/{id}/reschedule`

Implemented in Phase 7.4. Returns 201 with
`{ "original": SessionResponse, "replacement": SessionResponse }`.

```json
{
  "newDate": "2026-07-23",
  "newStartTime": "09:00",
  "newEndTime": "13:00",
  "lecturerUserId": 20,
  "reason": "Lecturer unavailable",
  "version": 0
}
```

Both endpoints require a nonblank reason (maximum 2000 characters, trimmed) and
nonnegative current `version`. Missing/invalid fields return 400; stale versions
return 409. Authorization, active branch/batch requirements, and active-branch
header validation match manual editing. Lecturers must have current access to the
batch; a replacement's named lecturer additionally needs eligibility on its date.
Unauthorized callers receive 403; missing sessions or named lecturers return 404.

Only SCHEDULED sessions without submitted attendance can transition. Completed,
already cancelled/rescheduled, or attendance-submitted sessions return 409. A
repeat request never creates another replacement or overwrites a reason: reload
the session to inspect the first request's outcome.

Cancellation preserves the session's dates, lecturer, content and generation
identity, sets CANCELLED and `cancellationReason`, and releases the active slot.
Rescheduling preserves these original details, sets RESCHEDULED and
`reschedulingReason`, then creates a new SCHEDULED session in the same batch with
`originalSessionId` pointing to the immediate original. Topic, classroom and
remarks are copied. The new lecturer is explicitly selected by `lecturerUserId`;
null/omission leaves the replacement unassigned. Supply the original lecturer ID
to retain it. Date/start/end are required and at least one must change. Changing
only lecturer/content uses the ordinary edit endpoint.

Replacement dates must fall within inclusive batch bounds; start must precede
end. Overlaps with other SCHEDULED/COMPLETED sessions in the same batch return
409. The original is excluded from this check because it is being retired;
adjacent intervals and same-day time changes are allowed. Replacement sessions
have no attendance or generation-origin pair. The original retains its source
pattern/date, preventing automatic regeneration of the retired slot.

Further rescheduling creates a chain: A → B → C. Each original has at most one
immediate replacement, enforced by V15's unique constraint. The existing
same-batch foreign key prevents moving history between batches. No deletion or
reactivation endpoint is provided.

Both operations share the batch lock with generation/manual editing. Original
transition, replacement insertion (when applicable) and audit are atomic.
SESSION_CANCELLED records before/after plus reason; SESSION_RESCHEDULED records
the original before value and both resulting responses plus reason. Any insertion
or audit failure rolls back the whole operation.

`ClassSession.requireAttendanceEligible()` rejects CANCELLED and RESCHEDULED
sessions. Phase 8 must call this guard after taking the same batch lock and
reloading the session for every attendance mutation. Attendance APIs and report
calculations are not implemented in this sub-phase. Phase 8 must also check saved
attendance rows (including drafts) before allowing lifecycle corrections; the
existing submitted-attendance field currently blocks such operations.

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

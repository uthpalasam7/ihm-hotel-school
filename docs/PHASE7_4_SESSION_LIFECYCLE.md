# Phase 7.4 — Session lifecycle

Implemented 4 October 2026. This sub-phase adds backend cancellation and
rescheduling; the corresponding user interface remains Phase 7.6.

## Behavior

`POST /api/v1/sessions/{id}/cancel` requires a reason and current version and
returns the retained session with status CANCELLED. Dates, lecturer, content,
creation fields and generated origin remain unchanged. The cancellation reason
and actor/time are recorded; the active time slot is released.

`POST /api/v1/sessions/{id}/reschedule` requires new date/start/end, reason and
current version. It returns 201 with `original` and `replacement` responses. The
original becomes RESCHEDULED with a saved rescheduling reason. A new SCHEDULED
session points to the original through `originalSessionId`. It copies topic,
classroom and remarks and has no attendance. Its lecturer is explicitly selected;
omitting the optional lecturer leaves it unassigned. At least one date/time value
must change; lecturer/content-only corrections use the ordinary edit API.

The same batch lock, current role/branch/lecturer assignment rules, date/time
validation and overlap prevention used by manual editing apply here. Each named
replacement lecturer must also be eligible on the proposed session date. The
branch must be active and the batch active/upcoming. Adjacent intervals and
same-day time changes are supported. Completed, cancelled, rescheduled and
submitted-attendance sessions reject lifecycle changes.

Each original has one immediate replacement. Subsequent moves form a chain
A → B → C. Generated origin stays on A, preventing generation from recreating its
retired slot. No record is hard-deleted or reactivated. Repeated/stale requests
return a conflict instead of overwriting reasons or creating extra replacements.

SESSION_CANCELLED audit evidence includes before/after and reason.
SESSION_RESCHEDULED includes the original before value, both resulting sessions
and reason. The transition, replacement insert and audit commit atomically;
insertion or audit failure restores the original state.

## Files changed

Under `backend/src/main/java/com/ihm/hotelschool/session/`:

- `ClassSession.java`: cancellation/rescheduling transitions, saved reason,
  replacement construction and `requireAttendanceEligible()` domain guard.
- `SessionWriteService.java`: lifecycle transactions, shared locked-session and
  version validation, replacement overlap/lecturer checks, atomic audit.
- `SessionController.java`: validated lifecycle endpoints.
- `SessionService.java` and `dto/SessionResponse.java`: expose rescheduling reason.
- New DTOs: `SessionCancellationRequest`, `SessionRescheduleRequest`,
  `SessionRescheduleResponse`.

Other files:

- `backend/src/main/resources/db/migration/V15__session_lifecycle.sql`.
- New `backend/src/test/java/com/ihm/hotelschool/session/SessionLifecycleTests.java`.
- `ManualSessionTests.java` and `ScheduleGenerationTests.java`: synthetic
  rescheduled fixtures now supply the mandatory reason.
- README, API specification, database design, implementation plan, testing
  requirements, and this report.

No frontend source files changed. The earlier 7.1–7.3 changes and staged
`.DS_Store` files remain untouched except for the feature extensions listed here.
No commit was created.

## Migration V15

- Adds `rescheduling_reason VARCHAR(2000)` and requires a nonblank value when
  status is RESCHEDULED.
- Enforces one immediate replacement with unique nullable `original_session_id`.
- Prevents replacement rows from copying the original's unique generation pair.
- Reuses the existing same-batch self foreign key, cancellation reason check,
  active-slot uniqueness and optimistic version.

Earlier migrations were not edited. Supported pre-7.4 APIs never created
RESCHEDULED rows or replacement links. If a database contains hand-inserted
rescheduled rows without reasons or duplicate replacement links, V15 will reject
that inconsistent data rather than invent reasons or delete history. Such data
needs an explicit repair before rollout. This task does not migrate development
records; integration verification uses disposable databases.

## Tests and commands

33 new lifecycle integration cases cover reason/version validation, authorization,
retained cancellation history, attendance eligibility, linked replacements and
chains, same-day moves, invalid dates/times, overlap/adjacency, named lecturer
eligibility, stale versions, terminal/submitted-attendance protection, generated
origin preservation, database constraints, insertion/audit rollback, simultaneous
reschedules, cancellation-versus-rescheduling and manual-create-versus-reschedule.

Verification passed:

- Full backend packaging: **186 tests**, zero failures/errors/skips, including
  the 33 new lifecycle cases.
- Session suites on H2: **90 tests** (33 lifecycle, 31 manual, 26 generation).
- The same **90 integration tests passed on PostgreSQL 17**, with all migrations
  including V15 applied to a fresh database. The disposable container was removed.
- Frontend build passed with its existing initial bundle warning: 1.15 MB against
  1.10 MB, exceeding the threshold by 54.22 kB. No frontend tests were rerun
  because frontend source did not change.
- Diff and untracked session-source/migration whitespace checks passed.

Commands, run from the relevant project directory:

```bash
./mvnw -q -Dtest=SessionLifecycleTests,ManualSessionTests,ScheduleGenerationTests test
./mvnw -q -Dtest=SessionLifecycleTests,ManualSessionTests,ScheduleGenerationTests \
  -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:53866/ihm_phase74_test \
  -Dspring.datasource.username=postgres -Dspring.datasource.password= \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver test
./mvnw -q package
npm run build
git diff --check
```

The PostgreSQL port was task-specific and the container is no longer running. Repeat these suites only against a fresh
disposable database: their committed fixtures clear application tables.

## Scope and follow-up

- Ordinary lifecycle changes remain restricted to unrecorded SCHEDULED sessions.
  A confirmed/audited correction flow for sessions with attendance is deferred.
- `requireAttendanceEligible()` rejects CANCELLED and RESCHEDULED; SCHEDULED and
  COMPLETED remain eligible subject to future attendance rules. Phase 8 must call
  this guard under the shared batch lock after reloading the session for every
  attendance write. It must also protect lifecycle changes when any attendance
  rows exist, including drafts, and test attendance-versus-cancellation races.
  There is no attendance write endpoint or attendance table yet; this report does
  not claim end-to-end attendance blocking or reporting is already implemented.
- Cross-batch resource scheduling is outside this sub-phase; overlap validation
  applies within the batch.
- UI confirmation and success/error states arrive with the lifecycle screens in
  7.6. No attendance, payment or email writes were introduced.
- Earlier Phase 6 physical-card and inbox/retry rollout checks remain pending.

Next bounded sub-phase: **7.5 — Regular schedule UI**, with weekly patterns,
preview and generation controls.

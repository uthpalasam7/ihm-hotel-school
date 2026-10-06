# Phase 7.2 — Regular generation

Completed 4 October 2026. This is the backend regular-schedule sub-phase; the
manual-session API is 7.3 and the weekly-pattern UI is 7.5.

## Delivered behavior

Administrators can list, create, update and deactivate weekly patterns in an
active/upcoming regular batch in an authorized active branch. Lecturers can read
patterns for currently assigned batches; regular generation is administrator-only.

A bounded preview expands ISO weekdays within inclusive batch dates, applies
exclusions, validates each named lecturer's assignment for that date, and reports
CREATE, ALREADY_GENERATED, EXISTING or CONFLICT for every proposed session. A
30-minute signed confirmation binds the actor, batch version, dates, exclusions
and active pattern versions/content. Generation requires that confirmation and
rechecks the plan under the batch lock. New conflicts reject the entire write;
concurrent or repeated generation skips sessions already present.

The transaction saves all new sessions and its audit event together. Pattern
changes do not rewrite sessions. Cancellation or rescheduling does not free a
previously generated pattern/date for automatic recreation. Deactivation preserves
pattern and session history. Existing matching manual sessions are retained, and
other overlapping active sessions in the batch are reported as conflicts.

Batch identity and dates cannot be edited in ways that reassign saved sessions or
leave them outside the batch dates. Switching to manual mode requires deactivating
active patterns. Batch updates and lecturer-assignment mutations share the same
batch lock used by generation and pattern changes.

## Files changed in this sub-phase

Under `backend/src/main/java/com/ihm/hotelschool/session/`:

- Added `ScheduleController.java`, `ScheduleService.java`,
  `SessionGenerationService.java`, `GenerationPreviewSigner.java`.
- Added DTOs `ScheduleRequest`, `ScheduleResponse`, `GenerationRequest`,
  `GenerationPreview`, `GenerationResult`.
- Updated `BatchSchedule`, `ClassSession`, their repositories, `SessionService`,
  and `SessionResponse` for mutation, generation provenance and response fields.

Supporting files:

- `batch/BatchService.java`, `CourseBatchRepository.java`,
  `BatchLecturerRepository.java`, and `user/UserRepository.java`.
- New `ScheduleGenerationTests.java` and `GenerationPreviewSignerTests.java`.
- README, API specification, database design, implementation plan, testing
  requirements, and this report.

The workspace also retains the earlier 7.1 files and edits; they were not reset.
Existing staged `.DS_Store` files were left untouched. No commit was created.

## Migrations

- **V13 — session generation origin:** nullable source-pattern/original-date pair,
  same-batch source foreign key, and unique source/date identity.
- **V14 — active schedule/session uniqueness:** PostgreSQL partial unique indexes
  for active pattern slots and SCHEDULED/COMPLETED session slots, including a null
  lecturer. H2 uses generated keys to enforce equivalent constraints in tests.

V12 remains unchanged. Normal backend startup applies V12–V14 if not already
applied. No migration was permanently applied to the development database by this
task; PostgreSQL verification used a separate disposable container.

## Time storage correction

The integration tests found that Hibernate's UTC JDBC timestamp setting also
shifted the default LocalTime mapping: 09:00 was physically stored as 03:30 under
a Colombo JVM. Both pattern and session time fields now use
`@JdbcTypeCode(SqlTypes.LOCAL_TIME)` so JDBC stores the local value directly.
Raw SQL and API tests cover early-morning and daytime times. Audit timestamps
retain UTC handling. Phase 7.1 exposed no writes, and this task does not rewrite
any externally inserted session rows.

## Validation results

- **122 backend tests passed**, zero failures/errors/skips, with successful
  backend packaging. This includes 26 new generation integration cases and one
  confirmation-signing test (27 new cases beyond the 95-test 7.1 baseline).
- **26 generation integration tests also passed on PostgreSQL 17**, including
  concurrent generation, concurrent pattern creation, database uniqueness with
  optional lecturers, transaction rollback, and raw local-time storage.
- The PostgreSQL run applied all Flyway migrations to a clean temporary database.
  Its container and temporary volume were removed after testing; development
  records were not used.
- Frontend build passed. The existing initial bundle warning remains: 1.15 MB
  against a 1.10 MB warning threshold, 54.22 kB over. No frontend files changed;
  frontend tests were not rerun.
- Whitespace/diff checks passed, including new untracked source files.

Commands (run from the corresponding project directory):

```bash
./mvnw -q -Dtest=ScheduleGenerationTests,GenerationPreviewSignerTests,SessionControllerTests test
./mvnw -q -Dtest=ScheduleGenerationTests,GenerationPreviewSignerTests test
./mvnw -q -Dtest=ScheduleGenerationTests \
  -Dspring.datasource.url=jdbc:postgresql://127.0.0.1:52064/ihm_phase72_test \
  -Dspring.datasource.username=postgres -Dspring.datasource.password= \
  -Dspring.datasource.driver-class-name=org.postgresql.Driver test
./mvnw -q package
npm run build
git diff --check
```

The PostgreSQL command used a task-created, loopback-only disposable container
with local test authentication. Port 52064 was dynamically assigned and is no
longer running; use a fresh test container/port when repeating the check. Never
point this integration test class at a development or production database: its
fixtures clear application tables.

## Assumptions and limits

- Maximum 100 active weekly patterns per batch, 366 days per request, 1000
  proposed sessions, 5000 existing context rows, and 1000 active assignments.
  Oversized work is rejected rather than silently truncated.
- Patterns may leave lecturer/classroom unassigned. When a lecturer is named,
  active role/branch membership and assignment on each new session date are required.
- Overlap checks in this step cover the same batch. Cross-batch classroom or
  lecturer resource scheduling is not introduced as an implicit business rule.
- The token covers proposed pattern details; duplicate/conflict outcomes are
  recomputed at confirmation. Already-created rows can become skips. The token
  is not an authentication token and does not bypass normal access checks.
- Pattern deletion means audited deactivation. No generated history is deleted.
- Future manual/lifecycle writes must use the same batch lock and preserve source
  generation identity; a replacement session must not steal the original's unique
  source/date pair.
- There are no new screens, attendance writes, emails or financial changes.
  The earlier Phase 6 physical/email rollout checks remain pending.

## Next bounded sub-phase

**7.3 — Manual sessions:** add authorized one-off creation and edit APIs, including
extra sessions in regular batches. Reuse the batch lock, local-time mapping,
active-slot constraints, current branch/assignment authorization, and overlap/date
validation. Add lecturer and administrator mutation tests, concurrency tests and
history protection before exposing these operations through the later UI.

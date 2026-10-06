# Phase 7.5 — Regular schedule UI

Implemented 4 October 2026. Administrators can configure and generate regular
class sessions from the batch screen. The next bounded task is 7.6, individual
session management UI.

## How the flow works

The Batches list links regular batches to `/batches/:id/schedule`. The route
admits `SUPER_ADMIN` and `ADMIN`; the backend APIs still enforce branch and role
authorization. The page loads the batch, a paginated list of weekly patterns and
its lecturer assignments. Manual and retired batches show their schedule details
but cannot preview or generate regular sessions.

An administrator adds or edits a pattern in a separate reactive-form dialog.
It accepts a weekday, forward 24-hour start/end times, an optional currently
assigned lecturer, optional classroom and status. It sends the pattern version
when editing. Active patterns can be deactivated after confirmation; previously
saved class sessions remain intact. API validation and conflict messages appear
in the dialog or page.

The administrator selects an inclusive range inside the batch, at most 366 days,
and can exclude individual dates such as holidays. `DD/MM/YYYY` input uses the
shared date adapter and is converted to ISO calendar dates for the API. Preview
does not save sessions. The response shows each proposed session as new, kept or
conflicting, with counts, messages and expiry. Changing a pattern, range,
exclusion or batch invalidates the preview. Conflicts, an empty set of new
sessions or an expired preview disable generation. After a confirmation dialog,
the page sends the exact reviewed range, exclusions and signed preview token to
the generation API. Server errors discard the stale preview and ask for a new
one; success shows created/kept counts. Duplicate sessions remain protected by
the existing Phase 7.2 backend and database constraints.

The weekly pattern list and bounded preview use Angular Material tables with
the shared `.ihm-data-grid` styling; on mobile they become cards. Patterns are
server-paginated. A single preview is bounded by the backend and paged locally
for display. Loading, empty, success, validation and request-error states are
visible. Route changes cancel obsolete reads and previews.

## Files changed

- `frontend/src/app/sessions/schedule.models.ts`, `schedule.service.ts` and
  `schedule-validators.ts`: typed API contracts, HTTP calls and form validation.
- `frontend/src/app/sessions/schedule-pattern-dialog.component.{ts,html,scss}`:
  pattern form and server feedback in separate template/style files.
- `frontend/src/app/sessions/batch-schedule.component.{ts,html,scss}`: schedule
  screen, paginated patterns, exclusions, preview review and confirmed generation.
- `frontend/src/app/app.routes.ts`, `batches/batch-list.component.html`: guarded
  route and batch-list entry points.
- Frontend specs for the service, dialog, page and batch-list role visibility.
- README, implementation plan, UI/testing requirements and this report.

No backend or database files were added or changed for 7.5. The backend and
V12–V15 migrations in the working tree belong to 7.1–7.4. The pre-existing
staged `.DS_Store` files were not part of this sub-phase. No commit was created.

## Verification

- `npm test -- --watch=false`: **158 tests in 34 files passed**.
- `npm run build`: passed. The initial bundle is 1.16 MB, 56.80 kB over the
  existing 1.10 MB warning budget; this is a warning, not a build error.
- In a disposable PostgreSQL 17 browser environment, signed in as a synthetic
  administrator, created a regular batch and Monday 09:00–13:00 pattern, excluded
  12 October 2026, previewed 3 new sessions with 0 conflicts, confirmed
  generation and saw 3 created/0 kept. Desktop and 390-pixel mobile layouts were
  visually checked. This used test-only records, separate ports and a disposable
  database, not the user's development data.
- `git diff --check` and source-file whitespace checks were run after the edits.

Earlier 7.4 verification remains recorded in its report: 186 backend tests and
90 session integration tests on disposable PostgreSQL passed. Backend behavior
did not change in this sub-phase.

## Follow-up

Phase 7.6 adds session list/calendar, manual/extra creation and editing,
cancellation and rescheduling screens. Phase 7.7 adds the lecturer view and
schedule regression checks. Phase 6 physical-card and SMTP inbox/retry rollout
checks still need school-side evidence.

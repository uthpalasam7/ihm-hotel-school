# IHM Hotel School Management System

The IHM Hotel School Management System has completed Phase 5 and started Phase 6 with enrollment, automatic charges, student QR cards, and optional card email.

## Stack

- Frontend: Angular 22, Angular Material 22.0.2, TypeScript, SCSS
- Backend: Java 17 target, Spring Boot 3.5.x, Maven Wrapper
- Database: PostgreSQL through Docker Compose
- Migrations: Flyway
- Initial timezone: Asia/Colombo
- Initial currency: LKR

The backend targets Java 17 and requires a JDK 17 or newer. A JRE alone is not enough because Maven needs `javac`.

## Repository Layout

```text
.
├── backend/
├── frontend/
├── docs/
├── branding/
├── docker-compose.yml
├── dev.sh
├── stop.sh
└── .env.example
```

## Local Setup

Copy the environment template and replace placeholder values for local use:

```bash
cp .env.example .env
```

Start PostgreSQL, the backend, and the frontend together:

```bash
./dev.sh
```

Stop all three services from another terminal (or after closing the terminal
that ran `dev.sh`):

```bash
./stop.sh
```

This stops this project's frontend and backend listeners and its PostgreSQL
container, while preserving the database volume. It is safe to run again if
they are already stopped. `Ctrl+C` in the `dev.sh` terminal also cleans up the
frontend and backend; PostgreSQL stays running until `./stop.sh` is run.
If a port belongs to a different program, `stop.sh` leaves that program alone.

The individual startup commands are available below for troubleshooting or when
only one service is needed.

Start PostgreSQL:

```bash
docker compose up -d
```

Run the backend:

```bash
cd backend
set -a
source ../.env
set +a
./mvnw spring-boot:run
```

For the first run, set these environment variables to create the first super administrator. The account is created only when no users exist, and it must change its password after login:

```bash
IHM_INITIAL_ADMIN_USERNAME=admin
IHM_INITIAL_ADMIN_PASSWORD=replace-with-temporary-password
IHM_INITIAL_ADMIN_FULL_NAME="Initial Super Administrator"
IHM_INITIAL_ADMIN_EMAIL=admin@example.invalid
IHM_JWT_SECRET=replace-with-at-least-64-random-characters
IHM_MAX_FAILED_LOGIN_ATTEMPTS=5
IHM_FAILED_LOGIN_LOCK_MINUTES=15
IHM_STUDENT_PHOTO_STORAGE_PATH=./data/student-photos
IHM_STUDENT_PHOTO_MAX_SIZE=5MB
```

Backend health checks:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

Authentication endpoints:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
GET  /api/v1/auth/me
POST /api/v1/auth/change-password
```

Repeated failed login attempts are rate-limited per username and remote address.

IntelliJ setup:

- Close any existing backend project window, then open `backend/pom.xml` as a
  Maven project. Do not import `backend/` as a plain Java project.
- Set the Project SDK to a JDK 17 or newer. A JRE is not enough because Maven needs `javac`.
- Use the Maven wrapper and the project settings under `backend/.mvn/`.
- Wait for Maven sync and indexing to finish before using code navigation.
- If the Spring Boot run configuration does not detect `HotelSchoolApplication`,
  close IntelliJ, delete the ignored `backend/.idea/` directory and
  `backend/hotel-school.iml`, then reopen `backend/pom.xml`.
- To run from IntelliJ, first start only PostgreSQL from the repository root with
  `docker compose up -d postgres`. Do not run `dev.sh` at the same time because
  it already starts a backend on port 8080.
- Create a Spring Boot run configuration for
  `com.ihm.hotelschool.HotelSchoolApplication`, use `backend/` as the working
  directory, and add the backend variables from the root `.env` file to the run
  configuration's environment variables.

Run the frontend:

```bash
cd frontend
npm start
```

The Angular dev server proxies `/api` and `/actuator` to `http://localhost:8080`, so keep the backend running while using the login page.

Open:

```text
http://localhost:4200
```

The authentication UI uses a responsive IHM split layout. Protected navigation
shows a reason on the Login page when sign-in is required or a session expires,
and authenticated users without a required role are sent to `/forbidden`.
Password changes retain the existing forced-change and sign-out flow.

## Build and Test

Backend:

```bash
cd backend
./mvnw test
./mvnw package
```

The Maven wrapper uses `backend/.mvn/settings.xml`, which resolves dependencies
from Maven Central and uses Maven's standard local cache at `~/.m2/repository`.
This keeps home and office repository settings from changing the project build
while remaining compatible with IntelliJ's Maven importer.

Frontend:

```bash
cd frontend
npm test -- --watch=false
npm run build
```

Infrastructure:

```bash
docker compose config
```

Student photos are stored outside the public frontend through a replaceable
backend storage abstraction. Local development uses `backend/data/student-photos`
by default; include this directory in local backup procedures.

## Implemented Scope: Phase 5 and First Phase 6 Delivery

Implemented:

- Responsive Angular Material application shell with IHM branding
- IHM Material 3 theme and shared page feedback patterns
- Dashboard branch context and role-authorized shortcuts without fake metrics
- Spring Boot backend scaffold
- Public backend health endpoint under `/api/v1/health`
- Actuator health endpoint
- PostgreSQL Docker Compose service
- Flyway configuration
- Environment-variable based configuration
- CI-ready test and build commands
- Roles, users, user-branch assignments, refresh-token storage
- JWT login, refresh, logout, current-user, and password-change endpoints
- Default roles and default branch seed data
- Environment-driven first-super-admin bootstrap
- Modernized Angular login and password-change screens, auth guard, interceptor, and role-aware Forbidden state
- Server-paginated Branch list and guarded Branch create/edit flow
- Branch create, edit, and activation/deactivation APIs
- Modernized Material User list and grouped create/edit flow with responsive
  presentation, role and branch assignment, guarded unsaved changes, account
  status confirmations, and one-time password handling
- Lecturer account creation by assigned-branch administrators
- Audit event persistence for branch and user administration actions
- Modernized Material Course list and guarded create/edit flow with
  server-side pagination and validation feedback
- Responsive Course batch list and guided add/edit wizard with preserved
  status-change APIs and branch authorization
- Branch-scoped batch filtering through the active branch context
- Batch fee-plan configuration with installment preview only
- Batch schedule-mode selection
- Batch lecturer assignment APIs and wizard step with multi-lecturer synchronization
- Global Student directory for `SUPER_ADMIN` and `ADMIN`
- Paginated Student search by name, normalized NIC, and contact number
- Student create, profile, edit, activation/deactivation, and duplicate-NIC lookup workflows
- Authenticated JPEG/PNG photo upload, replacement, thumbnail display, and removal
- Student audit events for profile, status, and photo changes
- Admin enrollment creation with a charge preview and server-generated registration number
- Atomic enrollment, fee snapshot, charge generation, and audit event
- Enrollment list, student and batch enrollment views, and saved charge details
- Branch authorization and prevention of duplicate or concurrent registration numbers
- Audited enrollment status changes with required reason and optimistic locking
- Read-only student rosters for lecturers with current assigned batches; no financial details
- One student-level QR card per student, with replacement, cancellation, audit history,
  and protected two-sided PDF printing/download
- Optional staff-confirmed card email to the saved student address, with durable
  delivery status and bounded retries when SMTP is configured

Not implemented yet:

- Remaining Phase 6 rollout checks: physical card and scanner validation and real SMTP delivery validation
- Follow [the Phase 6 rollout checklist](docs/PHASE6_ROLLOUT_CHECKLIST.md) when school hardware and a school SMTP account are available
- Class sessions, attendance, payment, report, and audit-view workflows

Current authorization:

- `SUPER_ADMIN` can manage all branches and all user roles.
- `ADMIN` can manage only `LECTURER` accounts in branches assigned to that admin.
- `ADMIN` can manage courses and only batches in assigned branches.
- `LECTURER` can list or view only currently assigned batches in authorized branches and view their minimal student rosters.
- `LECTURER` cannot access branch, user, course-management, batch-management, fee-plan, or lecturer-assignment mutation endpoints.
- Student identities are global rather than branch-owned. `SUPER_ADMIN` and `ADMIN`
  can manage them; active branch headers are validated and retained as audit
  context. Lecturers see only a minimal roster for currently assigned batches;
  shared student profiles remain restricted to administrators.

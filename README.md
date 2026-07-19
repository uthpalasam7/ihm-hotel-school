# IHM Hotel School Management System

Phase 4 provides the authentication foundation, branch and user administration, and course/batch management for the IHM Hotel School Management System.

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

Press `Ctrl+C` to stop the backend and frontend. PostgreSQL remains available for
later development sessions; stop it when needed with `docker compose down`.

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

- Open `backend/pom.xml` as a Maven project.
- Set the Project SDK to a JDK 17 or newer. A JRE is not enough because Maven needs `javac`.
- Use the Maven wrapper and the project settings under `backend/.mvn/`.
- If the Spring Boot run configuration does not detect `HotelSchoolApplication`, reimport the Maven project after setting the JDK.

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

The Maven wrapper uses `backend/.mvn/settings.xml`, which resolves dependencies from Maven Central and stores the local cache in `backend/.mvn/repository`. This keeps home and office Maven settings from changing the project build.

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

## Implemented Scope Through Phase 4

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
- User list, create, edit, role assignment, branch assignment, activation/deactivation, and password reset APIs and screens
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

Not implemented yet:

- Student, enrollment, attendance, payment, report, and audit-view workflows
- Automatic student charge generation; fee-plan previews are display-only until enrollment is implemented

Current authorization:

- `SUPER_ADMIN` can manage all branches and all user roles.
- `ADMIN` can manage only `LECTURER` accounts in branches assigned to that admin.
- `ADMIN` can manage courses and only batches in assigned branches.
- `LECTURER` can list or view only assigned batches in authorized branches.
- `LECTURER` cannot access branch, user, course-management, batch-management, fee-plan, or lecturer-assignment mutation endpoints.

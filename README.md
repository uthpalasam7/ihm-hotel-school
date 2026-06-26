# IHM Hotel School Management System

Phase 2 provides the authentication and authorization foundation for the IHM Hotel School Management System.

## Stack

- Frontend: Angular 22, TypeScript, SCSS
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
└── .env.example
```

## Local Setup

Copy the environment template and replace placeholder values for local use:

```bash
cp .env.example .env
```

Start PostgreSQL:

```bash
docker compose up -d
```

Run the backend:

```bash
cd backend
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

## Phase 2 Scope

Implemented:

- Angular application shell with IHM branding
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
- Angular login page, auth guard, interceptor, and role-aware navigation

Not implemented yet:

- Branch and user administration screens
- Course, batch, student, enrollment, attendance, finance, reports, and audit workflows

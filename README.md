# IHM Hotel School Management System

Phase 1 provides the project foundation for the IHM Hotel School Management System.

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

Backend health checks:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

Run the frontend:

```bash
cd frontend
npm start
```

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

## Phase 1 Scope

Implemented:

- Angular application shell with IHM branding
- Spring Boot backend scaffold
- Public backend health endpoint under `/api/v1/health`
- Actuator health endpoint
- PostgreSQL Docker Compose service
- Flyway configuration
- Environment-variable based configuration
- CI-ready test and build commands

Not implemented yet:

- Authentication and authorization
- Branch, user, course, batch, student, attendance, finance, reports, and audit workflows

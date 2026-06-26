# First Prompt for Codex

Read `AGENTS.md`, `README.md`, and every Markdown file under `docs/` before making changes.

This is a new IHM Hotel School Management System using:

- Angular and TypeScript for the frontend
- Java 17 and Spring Boot for the backend
- PostgreSQL for the database
- Flyway for database migrations
- Docker Compose for local infrastructure

Implement **only Phase 1** from `docs/IMPLEMENTATION_PLAN.md`.

Phase 1 must create:

- The repository folder structure
- A runnable Angular frontend
- A runnable Spring Boot backend
- PostgreSQL through Docker Compose
- Environment-variable configuration
- An `.env.example` file with placeholders only
- Basic health-check endpoints
- Initial frontend shell and branding
- Development documentation in `README.md`
- Initial automated build checks

Do not implement authentication, students, courses, attendance, fees, or reports yet.

Before coding:

1. Summarize the Phase 1 work you intend to perform.
2. List important assumptions.
3. Inspect the local environment and available tool versions.
4. Prefer stable supported versions.
5. Do not commit secrets.

After coding:

1. Run the backend tests.
2. Run the frontend tests.
3. Run both production builds.
4. Validate Docker Compose configuration.
5. Report changed files, commands run, results, limitations, and the next recommended phase.

Stop after Phase 1.

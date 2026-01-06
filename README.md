# Vacation Tool — Phase 0

## Repository layout
- `backend/`: Spring Boot app with auth, login/logout endpoints, RBAC filter, Flyway migrations.
- `frontend/`: Next.js app with SSR-friendly login page and validation.
- `docs/contract/`: Auth & RBAC contract document.
- `docker-compose.yml`: Local stack for backend + Postgres.

## Quickstart
1) Start services with Docker: `docker compose up --build`. Backend runs on `http://localhost:8080`, Postgres on `localhost:5432`.
2) Default demo credentials (seeded at startup): `demo` / `password`. Home route resolves to `/admin`.
3) Frontend: `cd frontend && npm install && npm run dev` (serves at `http://localhost:3000`). The login form posts to the backend at `http://localhost:8080/api/login`; adjust proxy or `NEXT_PUBLIC_API_BASE` if needed.

## Running locally without Docker
- Backend: `cd backend && mvn spring-boot:run` (requires JDK 17+, Maven). Configure env vars `DB_URL`, `DB_USER`, `DB_PASSWORD` for your Postgres instance.
- Database: Flyway migrations run on startup; schema is defined in `backend/src/main/resources/db/migrations/V1__create_user_role.sql`.
- Frontend: `cd frontend && npm install && npm test` then `npm run dev`.

## Testing
- Backend unit tests: `cd backend && mvn test`.
- Frontend unit tests: `cd frontend && npm test`.
- CI scripts live under `ci/scripts` and are wired in `.github/workflows/ci.yml` for build + login smoke coverage.

## Notes
- Session cookies are `HttpOnly`, `Secure`, `SameSite=Lax`, and bound to `SESSIONID`.
- RBAC filter protects `/api/**` endpoints and enforces admin/hr/manager path prefixes.

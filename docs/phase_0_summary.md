# Phase 0 Summary — Auth & RBAC foundation

## Deliverables
- Auth contract documented in `docs/contract/auth-rbac-contract.md` with semantic, data, interface, temporal, and verification sections plus open questions.
- Backend Spring Boot service with login/logout endpoints, role loading, session storage, and RBAC filter guarding `/api/**` paths.
- PostgreSQL schema + Flyway migration for `users`, `roles`, and `user_roles`, including seeded reference roles and optional demo user.
- Frontend Next.js login page and reusable `LoginForm` component with client-side validation, loading/duplicate submit guard, and error handling.
- CI workflow running backend + frontend tests and a login smoke test against a Docker-composed stack.

## How to run
- `docker compose up --build` brings up Postgres and the backend; health endpoint at `http://localhost:8080/health`.
- Default demo credentials: `demo` / `password` (can be disabled via `APP_SEED_DEMO_USER=false`).
- Frontend dev server: `cd frontend && npm install && npm run dev` (set `NEXT_PUBLIC_API_BASE=http://localhost:8080`).

## Notes & follow-ups
- Session cookie defaults to `HttpOnly` + `Secure` + `SameSite=Lax`; override `SESSION_COOKIE_SECURE` for HTTP-only local testing if needed.
- RBAC filter enforces admin/hr/manager prefixes; other `/api/**` routes require an authenticated session.
- CI scripts live under `ci/scripts` and are triggered via `.github/workflows/ci.yml`.

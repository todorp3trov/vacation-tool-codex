# Auth & RBAC Contract

## Semantic contract — Login flow
- **Trigger**: POST `/api/login` with `username`, `password`.
- **Guards**:
  - Reject empty username/password with `400`.
  - Lookup user by `username`; if not found or `status != ACTIVE`, return `401` with generic message.
  - Password verified with bcrypt (default) or Argon2 (configurable per ADR-002).
  - External dependencies: none for P0; only database access required.
- **State change**:
  - On success: create server-side session bound to `user_id`; load roles and store in session.
  - Set HttpOnly+Secure session cookie `SESSIONID` with SameSite=Lax; new session id issued on each login.
  - Persist `last_login_at` timestamp for auditing.
- **Outputs**:
  - `200` JSON `{ "homeRoute": "/{role-home}" }` where role-home is resolved by priority `ADMIN > HR > MANAGER > EMPLOYEE`.
  - Session cookie as above.
- **Failure behavior**:
  - Invalid credentials/disabled user: `401` JSON `{ "error": "Invalid username or password" }`; no session created.
  - Missing fields: `400` JSON `{ "error": "username and password are required" }`.
  - Server error: `500` JSON `{ "error": "Unexpected error" }` with no session mutation.

## Data contract — Schemas (EPIC-FR-001 aligned)
- **User** (source of truth in Postgres)
  - Fields: `id` UUID (pk), `username` varchar(120, unique, lowercase, trimmed), `password_hash` varchar(255), `display_name` varchar(140), `status` enum [`ACTIVE`,`DISABLED`] default `ACTIVE`, `created_at` timestamptz default now(), `updated_at` timestamptz default now(), `last_login_at` timestamptz nullable.
  - Invariants: username unique + case-insensitive; password_hash is bcrypt/Argon2 encoded; DISABLED users cannot log in; timestamps monotonic (`updated_at` >= `created_at`).
  - Serialization: API responses omit `password_hash`; status serialized as upper-case string; timestamps ISO-8601 UTC.
  - Versioning: v1.0; compatible with schema migration `V1__create_user_role.sql`.
- **Role**
  - Fields: `id` UUID (pk), `code` enum [`EMPLOYEE`,`MANAGER`,`HR`,`ADMIN`], `description` varchar(200) nullable, `created_at` timestamptz default now().
  - Invariants: `code` unique; roles are reference data.
  - Serialization: `code` upper-case string.
- **UserRole** (association)
  - Fields: `user_id` fk -> User.id, `role_id` fk -> Role.id, pk(user_id, role_id).
  - Invariants: no duplicate pairs; cascades delete on user removal.

## Interface contract — Public backend APIs
- **POST `/api/login`**
  - Request JSON: `{ "username": string, "password": string }`.
  - Responses: `200` with homeRoute payload + session cookie; `400` on validation; `401` on bad credentials/disabled; `500` on server error.
  - Cookie: `SESSIONID` HttpOnly, Secure, SameSite=Lax, Path=/; duration = server session timeout (configurable, default 30m).
- **POST `/api/logout`**
  - Behavior: invalidate current session (if any); respond `204` with cleared cookie (`Max-Age=0`).
- **RBAC guard**
  - Interceptor `INT-AUTH-RBAC` checks session for `user_id` and `roles`.
  - Protected endpoints require at least one of specified roles; otherwise `403` JSON `{ "error": "Forbidden" }`.
  - Missing/expired session returns `401` JSON `{ "error": "Unauthorized" }`.

## Temporal contract — Ordering, idempotency, concurrency
- Login sequence: validate input → authenticate user → hydrate roles → create session → respond with homeRoute.
- Role loading happens on login and on session refresh endpoints (future) to keep role cache current.
- Idempotency: repeated successful `/api/login` overrides previous session (new id issued); `/api/logout` is idempotent (multiple calls result in no active session).
- Concurrency: single user may hold multiple concurrent sessions; session invalidation is per cookie; DB uniqueness prevents concurrent username collision.

## Verification contract — Measurements and tests
- **Mapped tests**:
  - unit: `LoginControllerTest` covers success, missing fields (`400`), invalid credentials (`401`), disabled user (`401`), cookie flags present.
  - integration: `AuthSessionIntegrationTest` covers session creation, role storage, and logout invalidation.
  - unit: `RoleServiceTest` validates multi-role loading and effective permission calculation.
  - manual: `contract/validation/checklist` ensures doc parity with implementation and DB schema.
- **Metrics**: login success rate, failed logins count per username (rate-limiting TBD).
- **Open questions**:
  - Should we enforce max concurrent sessions per user?
  - Exact session timeout (default 30m) and remember-me token support?
  - Preferred password hashing between bcrypt and Argon2 for prod default?
  - Should login errors be rate-limited/capped per IP?

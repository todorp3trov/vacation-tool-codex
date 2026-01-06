## Phase 1: Employee Dashboard & Read-Only Data

Objectives
- Deliver the Employee Vacation Dashboard and Calendar read-only experience: calendar rendering, balance display (INT-001 read-only), teammates' vacations, IMPORTED Holidays overlay, and AuditLog writes for views.
- Implement session-cached balance reads per EPIC-FR-002 and ADR-003 (blocking display on unavailability).

Deliverables
- Backend endpoints to serve Employee dashboard data (vacation requests, teammates' approved/pending where applicable, imported holidays for range, session-cached balance retrieval).
- Frontend Employee Vacation Dashboard and Calendar UI (SSR where appropriate), vacation balance summary, and 'My Vacation Requests' list (read-only).
- AuditLog write for each dashboard access.
- Integration adapter for INT-001 and INT-004 reads with timeouts and retry policy per EPIC-FR-002 constraints, cached per session.

Tasks

- **P1-T01 — Backend: Dashboard API design & contract**
  - Description: Define and implement backend API endpoints used by the Employee dashboard: GET /api/employee/dashboard (returns session-cached official balance or unavailable flag, VacationRequest[] for employee, teammates' vacations if applicable, Holiday[] for visible range). Include response schema, pagination/filtering for calendar ranges, caching rules (session-based), and AuditLog side-effect semantics. Reference EPIC-FR-002 data_specification for entities.
  - depends_on: [P0-T01, P0-T03]
  - stories: [FR-002.1, FR-002.2, FR-002.3, FR-002.4]
  - files_modules:
    - backend/src/main/java/com/company/api/DashboardController.java
    - backend/src/main/java/com/company/dto/DashboardResponse.java
    - backend/src/main/java/com/company/service/DashboardService.java
  - tests:
    - unit:DashboardResponseSchemaTest
    - integration:DashboardApiAuthTest
  - acceptance:
    - GET /api/employee/dashboard returns DashboardResponse documented in contract including officialBalance (nullable), tentativeBalance (computed per session), vacations and holidays for requested range.
    - Endpoint writes a single AuditLog entry per call including actor_id and timestamp.

- **P1-T02 — Backend: Integration adapter INT-004 + INT-001 read (session caching)**
  - Description: Implement IntegrationConfig lookup (INT-004) and INT-001 client adapter using configured endpoint_url and auth headers. Apply retry/backoff (1s,2s,4s) and 3s timeout as required. Cache successful balance read in server-side session scoped storage for the session duration to avoid repeated calls. When integration is missing or errors after retries, return unavailable flag and do not set numeric balance.
  - depends_on: [P0-T01, P0-T03]
  - stories: [FR-002.2, FR-002.3, FR-002.9]
  - files_modules:
    - backend/src/main/java/com/company/integration/IntegrationConfigRepository.java
    - backend/src/main/java/com/company/integration/VacationBalanceClient.java
    - backend/src/main/java/com/company/service/BalanceSessionCache.java
  - tests:
    - unit:VacationBalanceClientTest (retry/timeouts simulated)
    - integration:BalanceSessionCacheTest
  - acceptance:
    - Balance client respects retry/backoff and timeout; on full failure returns unavailable.
    - Successful balance read is cached in session and served for subsequent dashboard requests in same session.

- **P1-T03 — Backend: Queries for VacationRequest, Holiday, TeamMembership for calendar ranges**
  - Description: Implement efficient DB queries to load an Employee's VacationRequests across lifecycle states for a date range, teammates' APPROVED/PENDING vacations when ACTIVE TeamMembership exists, and Holiday records with status = IMPORTED for visible range. Ensure queries are parameterized with date ranges and limited to needed fields for performance.
  - depends_on: [P0-T02, P0-T04]
  - stories: [FR-002.4, FR-002.7, FR-002.8, FR-002.11]
  - files_modules:
    - backend/src/main/java/com/company/repos/VacationRequestRepository.java
    - backend/src/main/java/com/company/repos/HolidayRepository.java
    - backend/src/main/java/com/company/repos/TeamMembershipRepository.java
  - tests:
    - unit:VacationRequestQueryTest
    - integration:CalendarRangeLoadTest
  - acceptance:
    - Queries return correct records for the provided date range and respect ACTIVE statuses for Team and TeamMembership.
    - Holidays with status DEPRECATED are excluded.

- **P1-T04 — Frontend: Employee Dashboard skeleton + calendar wiring (read-only)**
  - Description: Implement Employee Dashboard SSR page (Next.js) rendering Vacation Balance Summary, Employee Vacation Calendar (read-only), and My Vacation Requests list. Wire to /api/employee/dashboard to render server-side where possible and hydrate client. Calendar navigation triggers backend fetches for new date ranges (no mutations). The Balance Summary shows official/tentative label or unavailable error per API response.
  - depends_on: [P0-T05, P1-T01]
  - stories: [FR-002.1, FR-002.5, FR-002.6, FR-002.11]
  - files_modules:
    - frontend/pages/employee/dashboard.tsx
    - frontend/components/CalendarView.tsx
    - frontend/components/BalanceSummary.tsx
    - frontend/components/MyVacationList.tsx
  - tests:
    - unit:BalanceSummary.test
    - e2e:EmployeeDashboardReadOnlyTest
  - acceptance:
    - Dashboard shows either numeric official/tentative balance or a clear unavailable message.
    - Calendar displays vacation items with distinct styles per status and overlays IMPORTED holidays.
    - Navigating months triggers fetch/filter but does not change persisted data.

- **P1-T05 — Backend: AuditLog write for Dashboard views**
  - Description: Implement AuditLog persistence and ensure a single AuditLog entry is created for each Employee dashboard view attempt (success or when balance unavailable) containing actor_id, action_type=EMPLOYEE_DASHBOARD_VIEW, entity_type, entity_id (User.id or fixed dashboard UUID), timestamp, and details about balance availability.
  - depends_on: [P1-T01, P0-T03]
  - stories: [FR-002.10]
  - files_modules:
    - backend/src/main/java/com/company/repos/AuditLogRepository.java
    - backend/src/main/java/com/company/model/AuditLog.java
    - backend/src/main/java/com/company/service/AuditService.java
  - tests:
    - unit:AuditServiceDashboardTest
    - integration:DashboardAuditWriteTest
  - acceptance:
    - Each dashboard API call persists exactly one AuditLog with required fields.
    - AuditLog write failures are logged and do not prevent dashboard rendering.

- **P1-T06 — E2E: Read-only dashboard performance and blocking behavior test**
  - Description: Create automated e2e tests that validate: (a) when INT-001 is reachable returns numeric balance within 2s for typical cases, (b) when INT-001 unavailable dashboard shows unavailable message and still renders calendar & history, and (c) AuditLog entry written. Use test doubles for INT-001 to simulate success and failure.
  - depends_on: [P1-T02, P1-T04]
  - stories: [FR-002.3, FR-002.9, FR-002.10]
  - files_modules:
    - tests/e2e/dashboard-balance-success.spec.ts
    - tests/e2e/dashboard-balance-unavailable.spec.ts
  - tests:
    - e2e:dashboard-balance-success
    - e2e:dashboard-balance-unavailable
  - acceptance:
    - E2E test demonstrates numeric balance display when INT-001 returns success.
    - E2E test demonstrates unavailable message and calendar still loads when INT-001 fails.
    - AuditLog entry assertion present in both cases.

- **P1-T07 — Spike: Confirm session caching TTL & eviction strategy**
  - Description: Timeboxed spike (2d) to confirm session caching approach for balance values: in-memory session map vs storing cached balance in DB session table. Deliverable: recommendation doc (max 2 pages) and prototype showing eviction on session end and explicit cache invalidation hooks.
  - depends_on: [P0-T01]
  - stories: [FR-002.3]
  - files_modules:
    - docs/spikes/session-balance-cache.md
    - backend/src/test/java/com/company/spike/BalanceSessionCacheSpike.java
  - tests:
    - unit:BalanceCacheSpikeTest
  - acceptance:
    - Spike delivers recommendation and prototype with measured latencies and memory impact.
    - Decision recorded in contract doc and referenced in implementation tasks.
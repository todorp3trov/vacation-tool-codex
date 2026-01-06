# Pen-Clicker-v2 — Master Implementation Plan

## Glossary
| term | definition |
|---|---|
| Admin | The system administrator who configures the company setup, manages users and roles, and controls integrations such as national holiday imports. |
| Admin Integration Controls for National Holidays | The Admin tools that load national holidays from an external API by selecting a year and importing JSON, keeping holiday data current without manual entry. |
| AuditLog | A record of key actions such as vacation submission, approval, denial, and processing. The audit log supports traceability and helps resolve disputes about who did what and when. |
| Blocking Behavior on External System Downtime | The rule that if the external vacation-balance system cannot be reached, the Vacation Tool must prevent actions like submissions, approvals, or deductions that depend on it and instead show a clear error. For this EPIC, if the system is unreachable, the dashboard must not show numeric balances and must display a clear error message. |
| Display of National Holidays in Calendars | The feature that shows imported national holidays directly on employee and manager calendars. This visual context helps avoid planning vacations on days that are already public holidays. |
| DRAFT VacationRequest | A transient, in-memory representation of a vacation request being created by an Employee that has not yet been validated or persisted; it becomes PENDING only after passing minimum notice and external balance validations and being saved. |
| Employee | A regular worker who uses the Vacation Tool primarily to view their vacation balance and submit vacation requests. |
| Employee Vacation Dashboard and Calendar | The main screen for Employees that shows their remaining vacation days and an interactive calendar with holidays and teammates’ vacations. This dashboard is where employees understand their availability and initiate new vacation requests. |
| Enterprise Event Sink | An external message queue or event bus where the system publishes domain events such as TeamCreated, TeamArchived, TeamMembershipAdded, TeamMembershipRemoved, and user lifecycle events for consumption by other enterprise systems. |
| External Vacation-Balance System | A third-party system that serves as the official source of truth for employee vacation balances and receives deductions when VacationRequests are processed. |
| ExternalBalanceSystemUnavailable Event | A domain event emitted when an operation that depends on the External Vacation-Balance System (such as VacationRequest submission) fails because the system is unreachable or returns an error, used for monitoring and downstream handling. |
| Holiday | A national holiday imported from an external system and displayed on calendars so vacation planning can avoid conflicts with non-working days. Holidays improve accuracy of scheduling and visibility across employees and managers. |
| HolidayDeprecated Event | A domain event emitted when a previously imported Holiday record is marked as deprecated, including identifiers and metadata such as date and deprecation reason. |
| HolidayImported Event | A domain event emitted when national holidays for a specific year are successfully or partially imported, including information about the year, number of imported records, and timestamp. |
| HR | The Human Resources role that processes only manager-approved vacation requests, adds internal notes, and marks them as processed. |
| HR Notes (hr_notes) | Internal textual notes entered by HR users during processing of an approved VacationRequest, stored on the VacationRequest entity and not visible to Employees. |
| HR Processing Area for Approved Requests | A dedicated HR workspace that shows only manager-approved vacation requests, where HR can add internal notes and mark them as processed. It simplifies HR work by filtering out unapproved items and clarifying the final step in the flow. |
| Integration with External Vacation-Balance System | An API-based connection to a separate system that stores official vacation balances, used to read remaining days and deduct approved time off. This integration makes the external system the source of truth while still providing a smooth user experience in Vacation Tool. |
| IntegrationConfig | Configuration data that stores endpoints, credentials, and options for external systems such as vacation-balance and holiday APIs. It allows Admins to manage integrations without changing the core application logic. |
| IntegrationConfigured Event | A domain event emitted when an IntegrationConfig is created or updated in CONFIGURED state, including integration type, configuration identifier, actor, and timestamp. |
| IntegrationDisabled Event | A domain event emitted when an IntegrationConfig transitions from CONFIGURED to DISABLED, including integration type, configuration identifier, actor, and timestamp. |
| Manager | A people manager responsible for reviewing and deciding on vacation requests from their team members. |
| Manager Approval/Denial Workflow | The step where managers review pending vacation requests listed with key details and take approve or deny actions that are final from a business decision standpoint. This workflow is the authoritative gate between employee intent and HR processing. |
| Manager Calendar View with Pending Requests | A calendar view for Managers that shows their team members’ vacations and highlights pending requests. It helps managers make informed approval decisions by seeing overlapping absences in context. |
| Manager-Only View of Team Vacation Balances | A restricted view where only managers can see the remaining vacation days for each employee under their responsibility. This supports informed approval decisions without exposing balances to the entire organization. |
| Minimum Notice Rule Enforcement | The strict rule that all vacation requests must be submitted at least 14 calendar days before the requested start_date, enforced at submission time by comparing start_date to the current date. |
| National Holiday Data Import | The process of loading official holiday data for a given year from an external API into the Vacation Tool as JSON, triggered by an Admin action. |
| Pending Request Visualization and Tentative Balance Adjustment | The behavior where submitted but not yet approved vacation days appear as pending on the calendar and are tentatively subtracted from the employee’s balance. This gives all parties a realistic view of upcoming absences while still indicating that they are not final. |
| PENDING VacationRequest | A persisted vacation request that has been successfully submitted by the Employee and is awaiting Manager decision; it contributes to tentative balance adjustments until it is approved, denied, canceled, or processed by other workflows. |
| PROCESSED | A terminal VacationRequest lifecycle state indicating that HR has completed downstream processing for an approved request, including successful external balance deduction where required. |
| Role | A defined set of permissions (Employee, Manager, HR, Admin) that determines what a user can see and do in the Vacation Tool. |
| Role-Based Access Control | The security model in which the system shows and allows actions based on the user’s role, such as managers seeing team balances or HR seeing only approved requests. |
| Team | A logical grouping of users used to decide whose vacations are visible to each other in the calendar views. Teams structure the visibility and approval relationships in the Vacation Tool. |
| Team Vacation Visibility | The ability for employees to see their teammates’ scheduled vacations on their own calendar. This transparency helps teams coordinate absences and avoid understaffing periods. |
| TeamMembership | The link between a user and a team that determines which teammates’ vacations that user can see. It underpins the ‘team’ concept for visibility without exposing all company-wide data. |
| Tentative Balance Adjustment | The provisional subtraction of requested vacation days from an employee’s remaining balance immediately after submission, before manager approval. It shows employees and managers a more realistic remaining balance while keeping the deduction reversible. |
| User | Any person who logs into the Vacation Tool, including Employees, Managers, HR, and Admins, each with specific permissions. |
| User Authentication and Role-Based Access | The login mechanism where users enter an admin-defined username and password, and the system grants permissions based on their role so that each user only sees information relevant to their responsibilities. |
| Vacation Request | A formal request submitted by an employee to take time off for specific dates, which passes through submission, approval/denial, and HR processing states. It is the central business object that connects employees, managers, HR, and the external vacation-balance system. |
| VacationProcessed Event | A domain event emitted when HR successfully marks an APPROVED VacationRequest as PROCESSED and external deduction has succeeded, carrying identifiers and date information for downstream consumers. |
| VacationRequest | A formal request submitted by an employee to take time off for specific dates, which passes through submission, approval/denial, and HR processing states. For this EPIC, the key lifecycle transition is from DRAFT to PENDING upon successful validation and submission. |
| VacationRequest (Entity) | The system record that stores all data for a single vacation request, including dates, employee, status (pending, approved, denied, processed), and audit information. It enables tracking, visualization, and integration with external balance systems. |
| VacationRequested Event | A domain event emitted to the Enterprise Event Sink when a VacationRequest transitions from DRAFT to PENDING, carrying identifiers, requested date range, and requestedDays for downstream consumers. |

Domain: pen-clicker-v2

## Tech Stack
- frontend
- Next.js 14 with React and TypeScript, using server-side rendering for authenticated views and a login page that interacts with backend authentication APIs.
- backend
- Java 17 with Spring Boot, including Spring Security for session-based authentication and role-based authorization.
- persistence
- PostgreSQL 15 storing User and Role entities, including username, password_hash, status, and role assignments.
- platform_services
- Authentication and Authorization Module providing password hashing, session management, and RBAC evaluation
- build_targets
- Web browser clients accessing the Next.js frontend over HTTPS
- Next.js 14 with React and TypeScript, using server-side rendering for the authenticated Employee Vacation Dashboard and Calendar view.
- Java 17 with Spring Boot, exposing REST endpoints to power the Employee Vacation Dashboard and Calendar, including balance retrieval and loading of VacationRequest, Holiday, Team, and TeamMembership data.
- PostgreSQL 15 for storage and querying of User, VacationRequest, Holiday, Team, TeamMembership, IntegrationConfig, and AuditLog data required by the dashboard.
- Session-based authentication and authorization per ADR-002 to identify Employees and enforce Employee-only access to the dashboard.
- HTTP client library (e.g., Spring WebClient/RestTemplate) to call INT-001 using the configuration resolved from INT-004.
- Audit and Logging Service to persist AuditLog entries for dashboard views.
- Web browser clients accessing the Next.js frontend over HTTPS for desktop, tablet, and mobile form factors.
- Single-node deployment of the Spring Boot backend and PostgreSQL database as described in ADR-001.
- Next.js 14 with React and TypeScript used to implement the Employee Vacation Dashboard and Calendar, including the Vacation Request Creation Dialog and balance summary updates required by this EPIC.
- Java 17 with Spring Boot providing REST endpoints and domain services for VacationRequest submission, validation, external balance integration, event emission, tentative balance adjustment, and AuditLog creation.
- PostgreSQL 15 storing VacationRequest and AuditLog entities, as well as Holiday and IntegrationConfig referenced by this EPIC.
- Enterprise Event Sink (message queue or event bus) used for publishing VacationRequested and ExternalBalanceSystemUnavailable events.
- Integration Configuration Store or secrets vault used to load IntegrationConfig for the External Vacation-Balance System.
- External Vacation-Balance System REST API providing official remaining balances.
- Web browser clients accessing the Next.js frontend over HTTPS.
- Single-node backend runtime hosting Spring Boot application and PostgreSQL database.
- Next.js 14 with React and TypeScript, using shared calendar and table components to render Manager Calendar View with Pending Requests, Pending Requests List, and Request Detail and Decision Panel.
- Java 17 with Spring Boot-based REST API implementing Manager approval/denial endpoints, validation, and orchestration of AuditLog writes and event emission.
- PostgreSQL 15 storing User, Team, TeamMembership, VacationRequest, Holiday, and AuditLog entities used by this EPIC.
- Authentication and Authorization Module for session-based authentication and Role-Based Access Control.
- External Vacation-Balance Integration Service for accessing official balances via INT-001.
- Enterprise Event Sink integration for publishing VacationApproved and VacationDenied events via INT-003.
- Audit and Logging Service for managing AuditLog entries.
- Web browser clients for Manager, HR, Employee, and Admin roles via the Next.js frontend.
- Single-node backend deployment hosting the Spring Boot API and PostgreSQL database.
- Next.js 14 with React and TypeScript, using server-side rendered views and HTTP calls to backend REST APIs for HR Processing Area functionality.
- Java 17 with Spring Boot monolithic backend exposing REST APIs for HR processing, external system integration, and audit logging as per ADR-001.
- PostgreSQL 15 for storing VacationRequest and AuditLog data including hr_notes, processed_at, hr_id, and external_deduction_status.
- External Vacation-Balance System reachable via REST API (INT-001).
- Enterprise Event Sink (message broker) for domain events (INT-003).
- Integration Configuration Store for integration metadata (INT-004).
- Single-node deployment on a server or local machine running the Spring Boot backend and Next.js frontend.
- Next.js 14 with React and TypeScript used to render the Admin Configuration for Users, Roles, and Teams UI and interact with backend APIs.
- Java 17 with Spring Boot providing REST endpoints and business logic for Admin management of Users, Teams, and TeamMemberships, including validation and transaction management.
- PostgreSQL 15 used to store User, Role, Team, TeamMembership, and AuditLog entities; transaction boundaries for atomic configuration changes are enforced at the database level.
- Authentication and Authorization Module enforcing session-based authentication and RBAC for Admin-only endpoints.
- RBAC evaluation/caching service used to recalculate or invalidate permissions when Roles or statuses change.
- Team Vacation Visibility mapping service used to recalculate visibility data when Teams or TeamMemberships change.
- Enterprise Event Sink integration (AMQP-based message broker) used for non-blocking domain event publishing.
- Audit logging service responsible for persisting AuditLog entries.
- Single-node deployment bundling the Spring Boot backend and PostgreSQL database, with a separately deployed but co-located Next.js frontend.
- Next.js 14 with React and TypeScript for implementing the Admin Integration Controls for National Holidays UI and integration management UI components.
- Java 17 with Spring Boot providing REST endpoints for holiday import, deprecation, and IntegrationConfig lifecycle management.
- PostgreSQL 15 storing Holiday, IntegrationConfig, and AuditLog entities via the monolithic backend persistence layer.
- HTTP client library (e.g., Spring WebClient/RestTemplate) for calling External National Holiday API.
- Message broker supporting AMQP for Enterprise Event Sink to publish HolidayImported, HolidayDeprecated, IntegrationConfigured, and IntegrationDisabled events.
- Secrets or configuration store for IntegrationConfig secure credential handling.
- Single-node deployment including the monolithic Spring Boot backend and Next.js frontend, suitable for pilot environments.## Glossary

- Session — Server-side HTTP session established on successful authentication. Stored server-side and referenced via secure HTTP-only cookie per ADR-002.
- IntegrationConfig — Persisted configuration records indicating external integration endpoints, auth method and state (CONFIGURED/DISABLED) used for INT-001 and INT-002.
- AuditLog — Immutable record stored in PostgreSQL capturing actor_id, action_type, entity_type, entity_id, timestamp, and optional details for traceability.
- VacationRequest — Domain entity representing employee vacation requests with lifecycle statuses (DRAFT, PENDING, APPROVED, DENIED, CANCELED, PROCESSED).

## Architecture Decisions (ADRs)
| id | title | decision | implications |
|---|---|---|---|
| ADR-001 | Overall Deployment Architecture: Single-Node Monolith vs Decomposed Services | Adopt a single-node, two-tier architecture with a monolithic Spring Boot backend and a separate Next.js frontend for the MVP pilot. All backend responsibilities, including authentication, authorization, and RBAC, will reside in a single deployable backend connected to PostgreSQL, with the Next.js frontend communicating via RESTful JSON APIs over HTTPS. | Authentication and RBAC for this EPIC must be implemented within the monolithic Spring Boot backend.; The login and role-based landing flows will use a single backend process, simplifying session management and authorization.; Scaling and future service decomposition are deferred until after the pilot; current work should maintain clear internal module boundaries to support potential later extraction. |
| ADR-002 | Authentication and Session Management Strategy for Vacation Tool | Implement server-side HTTP session-based authentication using Spring Security with secure password hashing (bcrypt or Argon2) and per-password salts stored in PostgreSQL. Use secure, HTTP-only cookies to carry the session identifier, and enforce role-based authorization for all protected endpoints. | This EPIC’s authentication flow must use server-side sessions rather than stateless tokens or external SSO.; Password verification must always use the configured hashing algorithm and never compare plaintext passwords.; Frontend routing and conditional rendering must rely on session-backed role information validated on each request to protected resources. |
| ADR-003 | Handling External Vacation-Balance Integration with Blocking Behavior on Downtime | All balance-dependent operations for the Employee Vacation Dashboard and Calendar use synchronous, strongly blocking calls to the External Vacation-Balance System; when the system is unavailable, the dashboard must not show numeric balances and must communicate the unavailability clearly. | On dashboard load, the backend must call INT-001 to retrieve the Employee’s balance, and if the call fails after retries, the dashboard must display a balance-unavailable error and omit numeric values.; No offline or cached fallback balances may be used on this dashboard when the external system is unavailable, to avoid divergence from the source of truth.; Dashboard performance must consider INT-001 latency but is not required to meet submission, approval, or processing SLAs, which are handled in other EPICs. |
| ADR-003-FR-003 | Synchronous, Blocking Balance Validation for VacationRequest Submission | VacationRequest submissions implemented by this EPIC will perform a synchronous balance-read operation against the External Vacation-Balance System. If the external system is unavailable or returns an error after configured retries and timeouts, the submission will be blocked, no VacationRequest will be persisted, and an ExternalBalanceSystemUnavailable event will be emitted. | Submission latency for valid requests depends on the External Vacation-Balance System’s response time but must remain within overall UX expectations.; External system downtime will temporarily prevent new submissions and must be clearly communicated to users.; No local caching of official balances is introduced by this EPIC; the external system remains the source of truth. |
| ADR-EPIC-FR-004-01 | Use Existing Monolithic Backend and Session-Based Auth for Manager Approval/Denial | Implement all Manager Calendar View, Pending Requests List, and approval/denial workflows within the existing monolithic Spring Boot backend and rely on the shared session-based Authentication and Authorization Module to identify Managers and enforce RBAC. | Approval and denial endpoints must be implemented as REST handlers within the monolithic backend, sharing transaction and persistence layers with other features.; All Manager UI routes must rely on session cookies validated by the backend and must not embed authorization logic solely on the client side.; Future extraction of Manager workflows into separate services, if needed, will require refactoring but is outside the scope of this EPIC. |
| ADR-EPIC-FR-004-02 | Synchronous Use of External Balance System for Manager Views | Use synchronous balance-read operations from the existing External Vacation-Balance Integration Service when computing tentative balances in Manager Pending Requests List and Request Detail, and block approvals if the external system is unavailable. | Manager list and detail rendering must tolerate balance-read failures by clearly indicating unavailable balances instead of using stale or inferred values.; Approve actions must be disabled or rejected when the External Vacation-Balance System is unreachable, while deny actions remain available.; Performance of balance-dependent operations must remain within the 2-second constraint specified in the acceptance criteria under normal external system conditions. |## Phase 0: Auth & RBAC

Objectives
- Establish server-side session auth, secure password validation, User/Role persistence, and centralized RBAC enforcement (backend + frontend guards).
- Provide a minimal login page and SSR route protection so all subsequent features can assume authenticated requests and role-aware routing.
- Deliver semantic/data/interface/temporal/verification contracts required for safe implementation across all phases.

Deliverables
- Contract Readiness Gate (semantic, data, interface, temporal, verification contracts documented and agreed).
- Spring Boot auth module skeleton wired with Spring Security and session management (login/logout endpoints).
- Next.js login page (SSR) and client-side form with validations.
- PostgreSQL schema for User and Role (per EPIC-FR-001 data_specification) and simple repo layer.
- RBAC middleware/filters for backend endpoints and frontend route guards.
- CI pipeline with unit-test runner and basic end-to-end smoke test for login flow.

Tasks

- **P0-T01 — Contract Readiness Gate — Auth & RBAC**
  - Description: Produce the complete Contract required before implementation: Semantic contract (login flow: Trigger->Guards->State change->Outputs->Failure behavior), Data contract (authoritative schema using EPIC-FR-001 data_specification for User & Role, serialization, required/optional fields, defaults, invariants, versioning), Interface contract (public backend APIs: POST /api/login, POST /api/logout, session cookie semantics, RBAC guard behavior), Temporal contract (ordering for login→role-loading→routing, idempotency for login attempts, concurrency policy), Verification contract (measurement + mapped tests). Output: a single docs/contract/auth-rbac-contract.md containing all items. If any detail missing, the task must list explicit questions for product/PO.
  - depends_on: []
  - stories: [US-001, US-002, US-003, US-004, US-005, FR-001.1, FR-001.2, FR-001.3, FR-001.4, FR-001.5, FR-001.6, FR-001.7, FR-001.8, FR-001.9, FR-001.10]
  - files_modules:
    - docs/contract/auth-rbac-contract.md
    - backend/src/main/java/com/company/auth/ContractSpecification.java
    - backend/src/main/resources/db/schema-user-role.sql
  - tests:
    - contract/validation/checklist (manual review)
    - unit:ContractParsingTest (automated assertions on produced JSON/YAML contract)
  - acceptance:
    - Document includes a Semantic contract for the login flow with explicit failure behavior and error messages.
    - Data contract includes EPIC-FR-001 User and Role schemas (fields, required/optional, defaults) and serialization format.
    - Interface contract enumerates endpoints, signatures, cookie rules, and RBAC enforcement points.
    - Verification contract maps acceptance criteria to measurable tests; outstanding questions are listed.

- **P0-T02 — Repository, DB schema & local dev setup**
  - Description: Create monorepo skeleton with frontend (Next.js) and backend (Spring Boot) modules. Implement PostgreSQL schema scripts for User and Role per EPIC-FR-001 data_specification and a DB migration stub. Provide local docker-compose for single-node runtime (Spring Boot + Postgres) per ADR-001.
  - depends_on: [P0-T01]
  - stories: [FR-001.1, FR-006.1]
  - files_modules:
    - docker-compose.yml
    - backend/pom.xml (or build.gradle)
    - backend/src/main/resources/db/migrations/V1__create_user_role.sql
    - frontend/package.json
    - README.md
  - tests:
    - integration:local-compose-healthy (start compose and verify services up)
    - unit:DBSchemaValidationTest
  - acceptance:
    - docker-compose brings up Spring Boot and PostgreSQL and migrations run successfully.
    - Database contains User and Role tables matching EPIC-FR-001 fields.
    - Repo README documents local dev startup steps.

- **P0-T03 — Backend: Authentication endpoints + session store**
  - Description: Implement backend endpoints: POST /api/login and POST /api/logout using Spring Security with session creation. Implement UserRepository (JPA) reading User by username. Password verification uses bcrypt/Argon2 per ADR-002 (configurable). Session cookie set Secure + HttpOnly. Implement server-side session binding to User.id and roles.
  - depends_on: [P0-T01, P0-T02]
  - stories: [US-002, FR-001.3, FR-001.4, FR-001.9, FR-006.4]
  - files_modules:
    - backend/src/main/java/com/company/auth/LoginController.java
    - backend/src/main/java/com/company/auth/SecurityConfig.java
    - backend/src/main/java/com/company/repos/UserRepository.java
    - backend/src/main/java/com/company/model/User.java
    - backend/src/main/java/com/company/service/SessionService.java
  - tests:
    - unit:LoginControllerTest (successful + failure flows)
    - integration:AuthSessionIntegrationTest (end-to-end via localhost)
  - acceptance:
    - POST /api/login establishes server-side session on valid ACTIVE credentials and returns redirect target.
    - Invalid credentials or DISABLED users return generic error, no session created.
    - Session cookie contains HttpOnly and Secure flags.

- **P0-T04 — Backend: Role loading & RBAC guard**
  - Description: On successful authentication, load Role assignments for the user and store role list in session. Implement a centralized RBAC filter/interceptor (INT-AUTH-RBAC) that enforces role checks on protected endpoints and returns consistent authorization errors. Include unit tests for multi-role sessions (e.g., Manager+HR). Reference ADR-001 and ADR-002 in implementation notes.
  - depends_on: [P0-T01, P0-T03]
  - stories: [US-003, FR-001.5, FR-001.8, FR-004.1]
  - files_modules:
    - backend/src/main/java/com/company/security/RbacFilter.java
    - backend/src/main/java/com/company/service/RoleService.java
    - backend/src/main/java/com/company/model/Role.java
  - tests:
    - unit:RoleServiceTest
    - integration:RbacFilterIntegrationTest
  - acceptance:
    - Session contains loaded roles and effective permissions computed.
    - RBAC filter denies access to protected endpoints when roles missing and allows when present.
    - Multi-role sessions (Manager+HR) include both permissions.

- **P0-T05 — Frontend: Login page (Next.js SSR) + routing stub**
  - Description: Implement the public login page with username/password fields, client-side required-field validation, and single submit control. On successful authentication, perform redirect based on server-provided home route. Implement client-side prevention of duplicate submissions and loading state. Keep page SSR-friendly per Next.js SSR guidance.
  - depends_on: [P0-T01, P0-T02]
  - stories: [US-001, FR-001.1, FR-001.9]
  - files_modules:
    - frontend/pages/login.tsx
    - frontend/components/LoginForm.tsx
    - frontend/styles/login.css
  - tests:
    - unit:LoginForm.test (validation + submit disabled during in-flight)
    - e2e:LoginSmokeTest (headless browser to POST and assert redirect)
  - acceptance:
    - Login page displays required fields and client-side validation prevents empty submit.
    - Submit button shows loading and prevents duplicate submits.
    - Generic error from backend is shown as "Invalid username or password".

- **P0-T06 — CI, linting, and basic e2e smoke pipeline**
  - Description: Add CI pipeline steps for building backend and frontend, running unit tests, and a single headless e2e smoke test that attempts a full login flow against a test container. Ensure secrets/config are templated and not stored in repo.
  - depends_on: [P0-T02, P0-T03, P0-T05]
  - stories: [FR-001.4, FR-001.9]
  - files_modules:
    - .github/workflows/ci.yml
    - ci/scripts/start-test-environment.sh
    - ci/scripts/run-e2e.sh
  - tests:
    - ci:e2e-login-smoke
    - ci:unit (run all unit tests)
  - acceptance:
    - CI builds backend and frontend, runs unit tests, and executes the login e2e smoke test on merge.
    - Failing tests block merge; pipeline documents required environment variables.

---

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

---

## Phase 2: Request Creation & Manager Decisions

Objectives
- Implement DRAFT→PENDING creation flow, holiday-aware number_of_days calculation, minimum notice validation, submission-time synchronous balance validation (blocking behavior), tentative balance application and reversal, event emission (VacationRequested, ExternalBalanceSystemUnavailable), and Manager approval/denial workflows with authorization checks and events.

Deliverables
- Frontend Vacation Request Creation Dialog integrated with calendar selection and submission behavior.
- Backend submission endpoint that enforces minimum notice, calls INT-001 synchronously, persists VacationRequest as PENDING on success, emits events, and writes AuditLog.
- Tentative balance calculation logic and UI updates for Employee and Manager views.
- Manager Pending Requests List, Request Detail and Decision Panel, and approval/denial endpoints implementing validation, state transitions, AuditLog writes, and event emission.

Tasks

- **P2-T01 — Frontend: Calendar selection + Draft request modal**
  - Description: Enhance CalendarView to support click-and-drag or equivalent range selection to create an in-memory DRAFT VacationRequest with start_date, end_date, and preliminary inclusive day count. Open Vacation Request Creation Dialog showing computed number_of_days (pre-holiday exclusion) and preview of official/tentative balance (if available). Submit disabled initially until validations pass.
  - depends_on: [P1-T04]
  - stories: [FR-003.1, FR-003.13]
  - files_modules:
    - frontend/components/CalendarView.tsx
    - frontend/components/VacationRequestDialog.tsx
    - frontend/styles/vacation-dialog.css
  - tests:
    - unit:VacationRequestDialog.test
    - e2e:DraftSelectionFlow.spec.ts
  - acceptance:
    - Selecting a date range opens dialog with draft fields populated.
    - Dialog displays computed number_of_days before submission (pre-holiday exclusion).
    - Submit button disabled until validations pass and balance retrievable.

- **P2-T02 — Backend: Holiday-aware number_of_days computation service**
  - Description: Implement a deterministic service that computes number_of_days as inclusive days between start_date and end_date minus Holiday dates marked IMPORTED and configured as non-working. Expose as internal API used by frontend dialog (via endpoint GET /api/compute-days) and by submission server-side re-check.
  - depends_on: [P1-T03]
  - stories: [FR-003.2]
  - files_modules:
    - backend/src/main/java/com/company/service/DayCountService.java
    - backend/src/main/java/com/company/api/ComputeDaysController.java
  - tests:
    - unit:DayCountServiceHolidayExclusionTest
  - acceptance:
    - Service calculates correct counts for scenarios with single/multiple holidays, boundary dates and inclusive range behavior.
    - Matches examples in AC-007.

- **P2-T03 — Backend: Submission endpoint with validations and INT-001 check**
  - Description: Implement POST /api/vacation/submit which: (a) validates minimum 14-day notice; (b) computes number_of_days via DayCountService and re-checks; (c) resolves IntegrationConfig and invokes INT-001 synchronously with retries/timeouts per EPIC; (d) blocks submission and emits ExternalBalanceSystemUnavailable event if external unavailable; (e) rejects if number_of_days > external balance; (f) persists VacationRequest and transitions to PENDING setting submitted_at and request_id; (g) emits VacationRequested event after commit and writes AuditLog entry. Ensure event publishing is non-blocking per INT-003 constraints. Reference ADR-003-FR-003.
  - depends_on: [P0-T03, P1-T02, P2-T02]
  - stories: [FR-003.3, FR-003.4, FR-003.5, FR-003.6, FR-003.7, FR-003.10, FR-003.11, FR-003.12]
  - files_modules:
    - backend/src/main/java/com/company/api/VacationSubmissionController.java
    - backend/src/main/java/com/company/service/VacationRequestService.java
    - backend/src/main/java/com/company/integration/EventPublisher.java
  - tests:
    - unit:VacationSubmissionValidationTest
    - integration:SubmissionExternalDownTest
  - acceptance:
    - Submission is blocked when INT-001 unavailable; ExternalBalanceSystemUnavailable event emitted and no DB persistence occurs.
    - On success, VacationRequest persisted as PENDING with submitted_at and VacationRequested event emitted.
    - AuditLog entry recorded for successful submission.

- **P2-T04 — Backend: Tentative balance calculation and exposure**
  - Description: Implement server-side function to compute tentative balance = officialBalance - sum(number_of_days of all PENDING VacationRequests for employee). Expose in dashboard API and manager endpoints. Ensure tentative subtract/reverse behavior is derived (computed) rather than incremental where possible to avoid drift (mitigation in EPIC risks).
  - depends_on: [P1-T02, P1-T03]
  - stories: [FR-003.8, FR-003.9, FR-004.3]
  - files_modules:
    - backend/src/main/java/com/company/service/BalanceComputationService.java
    - backend/src/main/java/com/company/dto/TentativeBalanceDto.java
  - tests:
    - unit:TentativeBalanceComputationTest
  - acceptance:
    - Tentative balance equals officialBalance minus sum(PENDING requested days).
    - When a PENDING transitions to DENIED/CANCELED tentative reflects removal of that request.

- **P2-T05 — Frontend: Reflect tentative balance updates and calendar refresh on submission**
  - Description: After successful submission response, close dialog, refresh dashboard data and calendar to show new PENDING request and updated tentative balance. If submission blocked due to external unavailability or insufficient balance show clear errors in dialog and keep DRAFT in-memory.
  - depends_on: [P2-T03, P2-T04]
  - stories: [FR-003.13, FR-003.5, FR-003.6]
  - files_modules:
    - frontend/components/VacationRequestDialog.tsx
    - frontend/components/BalanceSummary.tsx
    - frontend/pages/employee/dashboard.tsx
  - tests:
    - e2e:SubmitRequestSuccess.spec.ts
    - e2e:SubmitRequestBlocked.spec.ts
  - acceptance:
    - Success: calendar shows new PENDING request and tentative balance updated.
    - Blocked submission: shows error and does not persist; dialog remains or shows clear message.

- **P2-T06 — Manager: Pending Requests List and Request Detail API + UI**
  - Description: Implement Manager APIs to load Pending Requests List and individual Request Detail including overlaps, holidays in range, and tentative balances per employee (calls INT-001 synchronously as needed). Implement frontend Manager Calendar View, Pending Requests table and Request Detail & Decision Panel UI (Approve/Deny controls). Approve action must perform validation per FR-004.5 including external reachability; Deny does not require external balance reachability. (References: ADR-EPIC-FR-004-01, ADR-EPIC-FR-004-02)
  - depends_on: [P0-T04, P1-T03, P2-T04]
  - stories: [FR-004.1, FR-004.2, FR-004.3, FR-004.4, FR-004.5, FR-004.6, FR-004.7, FR-004.8, FR-004.9, FR-004.10]
  - files_modules:
    - backend/src/main/java/com/company/api/ManagerController.java
    - frontend/pages/manager/calendar.tsx
    - frontend/components/PendingRequestsList.tsx
    - frontend/components/RequestDecisionPanel.tsx
  - tests:
    - integration:ManagerPendingListTest
    - e2e:ManagerApproveDenyFlow.spec.ts
  - acceptance:
    - Manager sees PENDING/APPROVED vacations and holidays; pending list includes tentative balances or unavailable indicators.
    - Approve transitions PENDING→APPROVED with AuditLog and VacationApproved event (only when INT-001 reachable).
    - Deny transitions PENDING→DENIED with AuditLog and VacationDenied event and reverses tentative balance.

- **P2-T07 — Event publishing reliability & retry strategy implementation**
  - Description: Implement EventPublisher with non-blocking publish after DB commit and retry policy per Enterprise Event Sink constraints. Ensure publish failures do not roll back core transaction; add logging/alert hooks for persistent failures. Events: VacationRequested, ExternalBalanceSystemUnavailable, VacationApproved, VacationDenied.
  - depends_on: [P2-T03, P2-T06]
  - stories: [FR-003.10, FR-003.11, FR-004.6, FR-004.8, FR-004.6]
  - files_modules:
    - backend/src/main/java/com/company/integration/EventPublisher.java
    - backend/src/main/java/com/company/integration/EventPublisherConfig.java
  - tests:
    - unit:EventPublisherRetryTest
    - integration:EventPublishNonBlockingTest
  - acceptance:
    - Events published asynchronously post-commit; failures logged and retried without rolling back the originating DB transaction.
    - Published events contain required fields (requestId, employeeId, dates, requestedDays).

---

## Phase 3: HR Processing & External Deduction

Objectives
- Implement HR-only processing UI and backend flow for APPROVED→PROCESSED transitions that performs synchronous deduction calls to external vacation-balance system with idempotency and rollback on failure, AuditLog writes and event emission (VacationProcessed, ExternalBalanceSystemUnavailable).

Deliverables
- HR Processing Area UI listing APPROVED not-yet-PROCESSED requests and a processing detail panel with hr_notes and Mark as Processed action.
- Backend processing API that verifies preconditions, calls INT-001 deduction endpoint synchronously with retries and idempotency key (VacationRequest.id), persists processed_at/hr_id and external_deduction_status on success, emits VacationProcessed event, and writes AuditLog entries. On failure, block transition and emit ExternalBalanceSystemUnavailable event.

Tasks

- **P3-T01 — Backend: HR processing API and transactional semantics**
  - Description: Implement POST /api/hr/process that: (a) re-validates VacationRequest is APPROVED and processed_at is null; (b) resolves IntegrationConfig and checks INT-001 availability; (c) calls external deduction synchronous endpoint with idempotency key = VacationRequest.id; (d) on success persists hr_notes, processed_at, hr_id, external_deduction_status=SUCCESS; (e) on deduction failure after retries blocks transition, emits ExternalBalanceSystemUnavailable, writes AuditLog with failure details and does not change VacationRequest. Ensure atomic transactional boundaries and idempotency behavior per FR-005.6–FR-005.9.
  - depends_on: [P2-T03, P2-T07]
  - stories: [FR-005.1, FR-005.3, FR-005.4, FR-005.5, FR-005.6, FR-005.7, FR-005.8, FR-005.9, FR-005.11, FR-005.12]
  - files_modules:
    - backend/src/main/java/com/company/api/HrProcessingController.java
    - backend/src/main/java/com/company/service/HrProcessingService.java
    - backend/src/main/java/com/company/integration/ExternalDeductionClient.java
  - tests:
    - integration:HrProcessingSuccessTest
    - integration:HrProcessingExternalFailureTest
  - acceptance:
    - Successful processing calls external deduction once and persists PROCESSED state with processed_at/hr_id and emits VacationProcessed event.
    - On external failure processing is blocked; ExternalBalanceSystemUnavailable event emitted; VacationRequest remains APPROVED; AuditLog records failure.

- **P3-T02 — Frontend: HR Processing Area UI and Mark as Processed flow**
  - Description: Implement HR Processing Area UI with Approved Requests Queue (server-backed), Request Processing Detail Panel to edit hr_notes and Mark as Processed action. Include confirmation dialog and show success / failure messages returned from backend. Ensure UI calls POST /api/hr/process with hr_notes and handles blocking errors (external unavailability) gracefully.
  - depends_on: [P2-T06, P3-T01]
  - stories: [FR-005.1, FR-005.3, FR-005.4, FR-005.11]
  - files_modules:
    - frontend/pages/hr/processing.tsx
    - frontend/components/ApprovedRequestsQueue.tsx
    - frontend/components/RequestProcessingPanel.tsx
  - tests:
    - e2e:HrProcessSuccess.spec.ts
    - e2e:HrProcessBlocked.spec.ts
  - acceptance:
    - HR queue lists only APPROVED & unprocessed requests.
    - Mark as Processed triggers confirmation, calls backend and on success removes the request from queue and updates history views.
    - On external failure shows clear error and leaves item in queue.

- **P3-T03 — Backend: Deduction idempotency & requestId mapping**
  - Description: Ensure ExternalDeductionClient sends VacationRequest.id as requestId idempotency key. Implement retry logic (up to 3 retries per FR-005 constraints) and durable logging to reconcile repeated attempts. Provide tests that simulate duplicate calls returning success after retries and ensure state transitions are idempotent.
  - depends_on: [P3-T01]
  - stories: [FR-005.6, FR-005.9]
  - files_modules:
    - backend/src/main/java/com/company/integration/ExternalDeductionClient.java
    - backend/src/main/java/com/company/integration/IdempotencyHelper.java
  - tests:
    - unit:IdempotencyKeyTest
    - integration:DeductionIdempotentSuccessTest
  - acceptance:
    - Deduction call includes requestId and retries do not produce duplicate processed state.
    - System can reconcile duplicate external successes without producing inconsistent PROCESSED transitions.

- **P3-T04 — Audit and Events for HR processing operations**
  - Description: Extend AuditService and EventPublisher to emit VacationProcessed and ExternalBalanceSystemUnavailable events for HR processing attempts. Ensure AuditLog entries recorded for access, attempt, success, and failure (FR-005.12). Event publishing non-blocking as previously implemented.
  - depends_on: [P2-T07, P3-T01]
  - stories: [FR-005.12]
  - files_modules:
    - backend/src/main/java/com/company/service/AuditService.java
    - backend/src/main/java/com/company/integration/EventPublisher.java
  - tests:
    - integration:HrAuditAndEventTest
  - acceptance:
    - AuditLog entries created for access and each processing attempt (success/failure).
    - VacationProcessed event contains required fields and published asynchronously.

- **P3-T05 — E2E & chaos tests for HR processing external failures**
  - Description: Create tests that simulate external vacation-balance system timeouts and failures to validate rollback, error messages, events emission, and AuditLog records per acceptance criteria AC-016 and AC-017.
  - depends_on: [P3-T01, P3-T02]
  - stories: [FR-005.7, FR-005.8, FR-005.9]
  - files_modules:
    - tests/e2e/hr-processing-failure.spec.ts
    - tests/e2e/hr-processing-success.spec.ts
  - tests:
    - e2e:hr-processing-success
    - e2e:hr-processing-failure
  - acceptance:
    - E2E confirms successful deduction transitions to PROCESSED and emits VacationProcessed.
    - E2E confirms failures block processing and emit ExternalBalanceSystemUnavailable events and AuditLog entries.

- **P3-T06 — Ops: monitoring & alerting hooks for persistent event publish failures**
  - Description: Add lightweight alerting/logging hooks for event publish failures and deduction failures. Provide an ops page or log collection instructions. Keep minimal for pilot (opt for simple logs + CI alert channels).
  - depends_on: [P2-T07]
  - stories: [FR-005.12]
  - files_modules:
    - backend/src/main/java/com/company/ops/EventPublishMonitor.java
    - ops/README-monitoring.md
  - tests:
    - unit:EventPublishMonitorTest
  - acceptance:
    - Failures in event publishing or deduction generate structured logs and documented alerting instructions.

---

## Phase 4: Admin & Integrations (Holidays)

Objectives
- Deliver Admin UIs and backend flows for managing Users, Roles, Teams, TeamMemberships, IntegrationConfig management, Holiday imports and deprecations, and domain events + AuditLog for admin actions.
- Ensure atomic transactions for admin operations and immediate RBAC/team visibility refresh.

Deliverables
- Admin console for Users and Teams management (create/edit/activate/disable, team membership management), atomic persistence, and RBAC cache invalidation hooks.
- Integration Controls UI for managing IntegrationConfig (holiday_api and vacation-balance) with secure credential handling.
- Holiday import flow calling External National Holiday API (INT-002), JSON parsing & validation, upsert of Holiday records as IMPORTED, deprecation endpoint, and events emission (HolidayImported/HolidayDeprecated/IntegrationConfigured/IntegrationDisabled).
- AuditLog and domain events for admin operations per EPIC-FR-006 and EPIC-FR-007.

Tasks

- **P4-T01 — Backend: Admin APIs for Users, Roles, Teams, TeamMemberships (atomic transactions)**
  - Description: Implement admin REST endpoints for CRUD on User, Team, and TeamMembership with transactional behavior for multi-entity operations (create User + TeamMemberships, update Role + memberships). Enforce backend RBAC (Admin only) using existing RBAC guard. On commit, trigger RBAC/visibility refresh hooks. Emit domain events (UserInvited/UserActivated/UserDisabled/TeamCreated/TeamArchived/TeamMembershipAdded/TeamMembershipRemoved) via EventPublisher and write AuditLog entries. Ensure username uniqueness check handles concurrency.
  - depends_on: [P0-T04, P0-T03]
  - stories: [FR-006.1, FR-006.2, FR-006.3, FR-006.4, FR-006.5, FR-006.6, FR-006.7, FR-006.8, FR-006.9, FR-006.10]
  - files_modules:
    - backend/src/main/java/com/company/api/AdminUserController.java
    - backend/src/main/java/com/company/service/AdminConfigurationService.java
    - backend/src/main/java/com/company/repos/TeamRepository.java
    - backend/src/main/java/com/company/service/RbacCacheInvalidator.java
  - tests:
    - integration:AdminAtomicTransactionTest
    - unit:AdminEndpointsAuthorizationTest
  - acceptance:
    - Atomic operations commit all related changes or roll back entirely on error.
    - Events emitted for admin actions and AuditLog entries created.
    - RBAC cache invalidation hook invoked after successful commit.

- **P4-T02 — Frontend: Admin console (Users, Teams, TeamMemberships)**
  - Description: Implement Admin UI pages: Users List with User Create/Edit Drawer, Teams and Memberships table, Team and TeamMembership Management Dialog. Wire to admin APIs, enforce admin-only access client-side (UI-level) and rely on backend guards for enforcement. Provide username uniqueness validation via backend call.
  - depends_on: [P4-T01]
  - stories: [FR-006.1, FR-006.2, FR-006.3, FR-006.5, FR-006.6]
  - files_modules:
    - frontend/pages/admin/users.tsx
    - frontend/components/UserDrawer.tsx
    - frontend/pages/admin/teams.tsx
    - frontend/components/TeamDialog.tsx
  - tests:
    - e2e:AdminCreateUser.spec.ts
    - unit:UserDrawer.test
  - acceptance:
    - Admin creates user with optional team membership; changes persist atomically and UI refreshes.
    - Archiving team marks it ARCHIVED and prevents new memberships via UI.

- **P4-T03 — Backend: IntegrationConfig management + secure credential storage**
  - Description: Implement IntegrationConfig entity and admin APIs to create/update/disable IntegrationConfig records for types holiday_api and vacation-balance. Persist credentials securely (e.g., encrypted fields) and emit IntegrationConfigured/IntegrationDisabled events and AuditLog entries. Ensure DISABLED configs are excluded from use by import or deduction flows and that attempts using disabled config fail fast.
  - depends_on: [P0-T02, P4-T01]
  - stories: [FR-007.11, FR-007.12, FR-007.3]
  - files_modules:
    - backend/src/main/java/com/company/api/IntegrationConfigController.java
    - backend/src/main/java/com/company/model/IntegrationConfig.java
    - backend/src/main/java/com/company/service/IntegrationConfigService.java
  - tests:
    - unit:IntegrationConfigValidationTest
    - integration:IntegrationConfigDisableFailFastTest
  - acceptance:
    - IntegrationConfig persists with encrypted credentials and emits IntegrationConfigured event on create/update.
    - Disabling config prevents imports and deductions and emits IntegrationDisabled event.

- **P4-T04 — Backend: Holiday import flow (INT-002) and upsert into Holiday table**
  - Description: Implement POST /api/admin/holidays/import that: (a) validates selected year per configurable allowed range; (b) resolves active holiday_api IntegrationConfig (fail fast if missing/disabled); (c) calls External National Holiday API (INT-002) with timeout 10s and up to 2 retries with 2s delay; (d) parse JSON payload in memory, validate mandatory fields (date, name), skip invalid records; (e) upsert Holiday records for valid entries as IMPORTED status; (f) determine outcome (success/partial/failure), write AuditLog and emit HolidayImported event containing year and counts. Ensure records immediately queryable.
  - depends_on: [P4-T03, P1-T03]
  - stories: [FR-007.2, FR-007.4, FR-007.5, FR-007.6, FR-007.7, FR-007.9, FR-007.10]
  - files_modules:
    - backend/src/main/java/com/company/api/HolidayImportController.java
    - backend/src/main/java/com/company/service/HolidayImportService.java
    - backend/src/main/java/com/company/repos/HolidayRepository.java
  - tests:
    - unit:HolidayPayloadValidationTest
    - integration:HolidayImportPartialSuccessTest
  - acceptance:
    - Import obeys timeout/retry rules and upserts valid holidays as IMPORTED.
    - Outcome recorded and HolidayImported event emitted with counts.
    - Imported holidays immediately visible to calendar queries.

- **P4-T05 — Frontend: Admin Integration Controls for National Holidays**
  - Description: Implement Admin UI for Integration Controls: Holiday Integration Status card, Year selector and Import button, Imported Holiday Years and Records table. Use secure masking for endpoint display and prevent repeated submissions. Display import outcomes and partial success details.
  - depends_on: [P4-T03, P4-T04]
  - stories: [FR-007.1, FR-007.2, FR-007.7, FR-007.9]
  - files_modules:
    - frontend/pages/admin/integrations/holidays.tsx
    - frontend/components/HolidayImportForm.tsx
    - frontend/components/ImportedHolidaysTable.tsx
  - tests:
    - e2e:HolidayImportSuccess.spec.ts
    - unit:HolidayImportForm.test
  - acceptance:
    - Admin can trigger import for a validated year and view import summary.
    - UI masks credentials and shows reachability indicator.
    - Partial imports show counts of imported vs skipped.

- **P4-T06 — Backend: Deprecate Holiday endpoint and event emission**
  - Description: Implement endpoint to mark Holiday.status from IMPORTED to DEPRECATED, accept optional deprecation reason, persist change, ensure deprecated holidays no longer appear in calendar queries, emit HolidayDeprecated event and write AuditLog entry.
  - depends_on: [P4-T04]
  - stories: [FR-007.8, FR-007.10]
  - files_modules:
    - backend/src/main/java/com/company/api/HolidayAdminController.java
    - backend/src/main/java/com/company/service/HolidayService.java
  - tests:
    - integration:HolidayDeprecationTest
  - acceptance:
    - Holiday transitions to DEPRECATED and is excluded from calendar queries.
    - HolidayDeprecated event emitted and AuditLog entry recorded.

- **P4-T07 — Admin: RBAC cache invalidation & visibility refresh integration test**
  - Description: Implement integration test that changes a User role/team via Admin API and verifies RBAC and team visibility refresh takes effect on next login or page load (e.g., role change from Employee->Manager results in Manager landing view). Ensure RbacCacheInvalidator invoked by Admin service.
  - depends_on: [P4-T01, P0-T03]
  - stories: [FR-006.8]
  - files_modules:
    - tests/integration/admin-rbac-refresh.spec.ts
  - tests:
    - integration:AdminRbacRefresh
  - acceptance:
    - Role/team changes via Admin immediately cause RBAC/visibility refresh; subsequent login routes user to new home view.

---

## Story Coverage Index

| story_id | covered_by_tasks | key_tests |
|---|---:|---|
| US-001 | P0-T01, P0-T05 | e2e:LoginSmokeTest, unit:LoginForm.test |
| US-002 | P0-T03 | integration:AuthSessionIntegrationTest, unit:LoginControllerTest |
| US-003 | P0-T04 | integration:RbacFilterIntegrationTest, unit:RoleServiceTest |
| US-004 | P0-T03, P0-T04, P0-T05 | e2e:LoginSmokeTest |
| US-005 | P0-T04 | integration:RbacFilterIntegrationTest |
| FR-001.1 | P0-T01, P0-T05 | unit:LoginForm.test, e2e:LoginSmokeTest |
| FR-001.2 | P0-T03 | unit:LoginControllerTest |
| FR-001.3 | P0-T03 | unit:LoginControllerTest |
| FR-001.4 | P0-T03, P0-T06 | ci:e2e-login-smoke, integration:AuthSessionIntegrationTest |
| FR-001.5 | P0-T04 | unit:RoleServiceTest |
| FR-001.6 | P0-T03, P0-T04 | e2e:LoginSmokeTest |
| FR-001.7 | P0-T04, P0-T05 | integration:RbacFilterIntegrationTest, e2e:RoleBasedNavTest |
| FR-001.8 | P0-T04 | integration:RbacFilterIntegrationTest |
| FR-001.9 | P0-T03, P0-T05 | e2e:LoginSmokeTest |
| FR-001.10 | P0-T03 | unit:LoginControllerTest |
| FR-002.1 | P1-T04 | e2e:EmployeeDashboardReadOnlyTest |
| FR-002.2 | P1-T01, P1-T02 | unit:VacationBalanceClientTest |
| FR-002.3 | P1-T02, P1-T06 | e2e:dashboard-balance-success, ci:e2e-login-smoke |
| FR-002.4 | P1-T03, P1-T04 | integration:CalendarRangeLoadTest |
| FR-002.5 | P1-T04, P2-T04 | unit:TentativeBalanceComputationTest, e2e:SubmitRequestSuccess.spec.ts |
| FR-002.6 | P1-T04 | e2e:EmployeeDashboardReadOnlyTest |
| FR-002.7 | P1-T03, P1-T04 | integration:CalendarRangeLoadTest |
| FR-002.8 | P1-T03, P4-T04 | unit:HolidayPayloadValidationTest, e2e:EmployeeDashboardReadOnlyTest |
| FR-002.9 | P1-T02, P1-T06 | e2e:dashboard-balance-unavailable |
| FR-002.10 | P1-T05 | integration:DashboardAuditWriteTest |
| FR-002.11 | P1-T04 | e2e:EmployeeDashboardReadOnlyTest |
| FR-003.1 | P2-T01 | e2e:DraftSelectionFlow.spec.ts |
| FR-003.2 | P2-T02 | unit:DayCountServiceHolidayExclusionTest |
| FR-003.3 | P2-T03 | unit:VacationSubmissionValidationTest |
| FR-003.4 | P2-T03, P1-T02 | integration:SubmissionExternalDownTest |
| FR-003.5 | P2-T03, P2-T07, P2-T05 | integration:SubmissionExternalDownTest, e2e:SubmitRequestBlocked.spec.ts |
| FR-003.6 | P2-T03, P2-T05 | unit:VacationSubmissionValidationTest |
| FR-003.7 | P2-T03 | integration:SubmissionExternalDownTest |
| FR-003.8 | P2-T04, P2-T05 | unit:TentativeBalanceComputationTest, e2e:SubmitRequestSuccess.spec.ts |
| FR-003.9 | P2-T04, P2-T06 | integration:ManagerApproveDenyFlow |
| FR-003.10 | P2-T03, P2-T07 | integration:EventPublishNonBlockingTest |
| FR-003.11 | P2-T03, P2-T07 | integration:SubmissionExternalDownTest |
| FR-003.12 | P2-T03 | integration:SubmissionAuditTest |
| FR-003.13 | P2-T01, P2-T05 | e2e:SubmitRequestBlocked.spec.ts |
| FR-004.1 | P2-T06, P0-T04 | integration:ManagerPendingListTest |
| FR-004.2 | P2-T06 | e2e:ManagerApproveDenyFlow.spec.ts |
| FR-004.3 | P2-T06, P2-T04 | integration:ManagerPendingListTest |
| FR-004.4 | P2-T06 | integration:ManagerDetailTest |
| FR-004.5 | P2-T06, P2-T03 | e2e:ManagerApproveDenyFlow.spec.ts |
| FR-004.6 | P2-T06, P2-T07 | integration:ManagerApproveTest |
| FR-004.7 | P2-T06 | integration:ManagerDenyTest |
| FR-004.8 | P2-T06, P2-T04, P2-T07 | e2e:ManagerDenyFlow.spec.ts |
| FR-004.9 | P2-T06 | integration:ManagerAuditTest |
| FR-004.10 | P0-T04, P2-T06 | integration:AuthorizationNegativeTest |
| FR-005.1 | P3-T01, P3-T02 | integration:HrProcessingAccessTest |
| FR-005.2 | P3-T02, P3-T01 | integration:HrQueueFilterTest |
| FR-005.3 | P3-T02, P3-T01 | e2e:HrProcessSuccess.spec.ts |
| FR-005.4 | P3-T01 | integration:HrProcessingSuccessTest, integration:HrProcessingExternalFailureTest |
| FR-005.5 | P3-T01 | integration:HrProcessingValidationTest |
| FR-005.6 | P3-T01, P3-T03 | integration:DeductionIdempotentSuccessTest |
| FR-005.7 | P3-T01, P3-T04, P3-T05 | e2e:hr-processing-failure.spec.ts |
| FR-005.8 | P3-T01, P3-T04, P3-T05 | e2e:hr-processing-success.spec.ts |
| FR-005.9 | P3-T01, P3-T03 | integration:HrProcessingExternalFailureTest |
| FR-005.10 | P3-T01, P0-T04 | integration:HrForbiddenTransitionsTest |
| FR-005.11 | P3-T01, P3-T02 | e2e:HrProcessSuccess.spec.ts |
| FR-005.12 | P3-T04 | integration:HrAuditAndEventTest |
| FR-006.1 | P4-T01, P4-T02 | integration:AdminAtomicTransactionTest |
| FR-006.2 | P4-T01, P4-T02 | e2e:AdminCreateUser.spec.ts, integration:AdminAtomicTransactionTest |
| FR-006.3 | P4-T01 | integration:AdminAtomicTransactionTest |
| FR-006.4 | P4-T01, P0-T03 | unit:LoginControllerTest, integration:AdminAtomicTransactionTest |
| FR-006.5 | P4-T01, P4-T02 | integration:AdminTeamArchiveTest |
| FR-006.6 | P4-T01, P4-T02 | integration:AdminMembershipTest |
| FR-006.7 | P4-T01 | integration:AdminAtomicTransactionTest |
| FR-006.8 | P4-T01, P4-T07 | integration:AdminRbacRefresh |
| FR-006.9 | P4-T01 | integration:AdminAuditTest |
| FR-006.10 | P4-T01 | integration:EventPublishNonBlockingTest |
| FR-007.1 | P4-T05, P4-T03 | e2e:HolidayImportSuccess.spec.ts |
| FR-007.2 | P4-T04, P4-T05 | unit:HolidayPayloadValidationTest |
| FR-007.3 | P4-T03, P4-T04 | integration:IntegrationConfigDisableFailFastTest |
| FR-007.4 | P4-T04 | integration:HolidayImportPartialSuccessTest |
| FR-007.5 | P4-T04 | unit:HolidayPayloadValidationTest |
| FR-007.6 | P4-T04 | integration:HolidayImportPartialSuccessTest |
| FR-007.7 | P4-T04, P1-T03 | integration:HolidayImportPartialSuccessTest |
| FR-007.8 | P4-T06 | integration:HolidayDeprecationTest |
| FR-007.9 | P4-T04, P4-T05 | integration:HolidayImportPartialSuccessTest |
| FR-007.10 | P4-T04, P4-T06 | integration:EventPublishNonBlockingTest |
| FR-007.11 | P4-T03 | unit:IntegrationConfigValidationTest |
| FR-007.12 | P4-T03 | integration:IntegrationConfigDisableFailFastTest |

(End of Story Coverage Index)
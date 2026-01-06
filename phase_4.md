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
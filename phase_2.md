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
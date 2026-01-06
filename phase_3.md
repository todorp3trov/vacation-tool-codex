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
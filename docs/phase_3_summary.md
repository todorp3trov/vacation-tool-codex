# Phase 3 Summary — HR Processing & External Deduction

## Delivered
- HR processing API (`/api/hr/queue`, `/api/hr/request/{id}`, `/api/hr/process`) that validates APPROVED+unprocessed requests, writes hr_notes, sets processed_at/hr_id/external_deduction_status, and emits `VacationProcessed`/`ExternalBalanceSystemUnavailable` events.
- External deduction client with idempotency key (`VacationRequest.id`), three-attempt retry, and monitoring hook; updates `VacationRequest` schema with processed/deduction fields and audit entries for attempts/success/failure.
- HR Processing UI (queue + detail panel) that lists approved, unprocessed requests, supports confirmation + Mark as Processed flow, and surfaces external failure errors without dropping the item.
- Monitoring docs and hooks via `EventPublishMonitor` for event publish and deduction failures; added backend + frontend tests for success/failure paths and idempotent retries.

## Notes & follow-ups
- Deduction client posts to the configured INT-001 endpoint; adjust if a distinct path is required in your environment.
- Event publishing still uses the log-based sink; wire to the real transport when available.
- Consider adding more exhaustive integration/e2e coverage with a real INT-001 stub when test infrastructure allows.

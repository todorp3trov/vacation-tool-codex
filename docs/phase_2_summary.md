# Phase 2 Summary — Request Creation & Manager Decisions

## Delivered
- Holiday-aware `DayCountService` with GET `/api/compute-days` for frontend draft validation and server-side recalculation.
- Vacation submission flow `/api/vacation/submit` enforcing 14-day notice, external balance reachability, holiday-adjusted day counts, balance checks, audit logging, and non-blocking events (`VacationRequested`, `ExternalBalanceSystemUnavailable`).
- Tentative balance computation service derived from official balance minus summed pending requests; dashboard now uses this shared service.
- Manager endpoints for calendar data, pending list, request detail (with holidays/overlaps), and approve/deny transitions publishing `VacationApproved`/`VacationDenied` events.
- Event publisher infrastructure with post-commit async dispatch and retry, plus new request lifecycle audit actions.
- UI: employee calendar range selection and request dialog with day-count preview, notice rule messaging, and submission error handling; auto-refresh of dashboard on success.
- UI: manager calendar view, pending list, and decision panel with overlaps/holiday context and approval/denial actions.
- Unified `/api/calendar` + `/calendar` UI route shared by all roles; manager-only sections surface conditionally from the same endpoint.

## Notes & follow-ups
- Balance/tentative data is fetched synchronously for manager views; consider caching per user to reduce INT-001 calls on large queues.
- EventPublisher currently logs payloads; wire to the real sink when available.
- No automated tests added for new flows in this iteration; add coverage for day-count edge cases, submission validation, and manager approvals in the next cycle.

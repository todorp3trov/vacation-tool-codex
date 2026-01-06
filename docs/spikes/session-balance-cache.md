# Session Balance Cache — Spike Notes

## Goal
Identify a low-latency, session-scoped cache for INT-001 balance reads that:
- avoids repeated external calls within a browser session,
- evicts on logout/session expiry,
- keeps behavior fail-closed when the external system is down.

## Options considered
- **HttpSession attribute (in-memory, per-JVM)** — store balance snapshot + userId on login. Eviction handled automatically when the session expires (30m) or is invalidated on logout. Minimal code, zero extra storage, aligns with ADR-002 session model. Downside: not shareable across nodes without sticky sessions.
- **DB-backed session table** — persist balance per session row. Survives JVM restarts and could serve clustered deployments without sticky sessions. Adds writes per request and requires cleanup job to remove expired rows.
- **Distributed cache (Redis)** — good for clustering but adds infrastructure not planned for Phase 1.

## Recommendation
Use the **HttpSession attribute** approach for Phase 1:
- Cache record: `{ userId, balance, unavailableFlag, cachedAt }`.
- Set on successful INT-001 call; set `unavailableFlag=true` on failures to avoid hammering the integration during the same session.
- Eviction: implicit via session timeout (30m) and explicit on logout/session reset.
- Invalidation hooks: clear the cache when a different user logs into the same session or when we add balance-changing flows (Phase 2+).

## Prototype notes
- Implemented in `BalanceSessionCache` service with `ATTR_BALANCE_CACHE` stored on `HttpSession`.
- Dashboard service reads cache first; on miss it calls INT-001 with retry/backoff (1s/2s/4s, 3s timeout) and writes the snapshot.
- This added ~0ms overhead per call in local profiling; cache hit avoided the external round trip entirely.

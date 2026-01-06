# Monitoring & Alerting (Pilot)

- **Event publishing**: `EventPublishMonitor` logs permanent event publish failures at `ERROR` with the event name and attempt count. Ship these logs to your log collector (e.g., CloudWatch, ELK) and alert on occurrences of `EventPublishMonitor` messages.
- **External deductions**: `ExternalDeductionClient` records `ERROR` logs via `EventPublishMonitor.recordDeductionFailure` when INT-001 remains unavailable after retries. Alert on `External deduction failed after retries` lines to flag integration downtime.
- **Application logs**: Ensure backend logs are scraped from the Spring Boot container/pod. Minimal configuration is required beyond routing `ERROR` level logs to your alert channel.
- **Local verification**: Run the backend with `SPRING_PROFILES_ACTIVE=local` and trigger HR processing; confirm monitor logs appear in the console for simulated failures.

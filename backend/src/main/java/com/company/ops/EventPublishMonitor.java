package com.company.ops;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventPublishMonitor {
    private static final Logger log = LoggerFactory.getLogger(EventPublishMonitor.class);

    public void recordEventFailure(String eventType, int attempts) {
        log.error("Event publish permanently failed: event={} attempts={}", eventType, attempts);
    }

    public void recordDeductionFailure(UUID requestId, String reason) {
        log.error("External deduction failed after retries: requestId={} reason={}", requestId, reason);
    }
}

package com.company.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import com.company.ops.EventPublishMonitor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final Executor eventPublisherExecutor;
    private final EventPublishMonitor eventPublishMonitor;

    public EventPublisher(@Qualifier("eventPublisherExecutor") Executor eventPublisherExecutor,
                          EventPublishMonitor eventPublishMonitor) {
        this.eventPublisherExecutor = eventPublisherExecutor;
        this.eventPublishMonitor = eventPublishMonitor;
    }

    public void publishPostCommit(String eventType, Map<String, Object> payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            dispatch(eventType, payload, 0);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                dispatch(eventType, payload, 0);
            }
        });
    }

    public void publishImmediate(String eventType, Map<String, Object> payload) {
        dispatch(eventType, payload, 0);
    }

    private void dispatch(String eventType, Map<String, Object> payload, int attempt) {
        eventPublisherExecutor.execute(() -> attemptPublish(eventType, payload, attempt));
    }

    private void attemptPublish(String eventType, Map<String, Object> payload, int attempt) {
        int maxAttempts = 3;
        try {
            // Placeholder for actual sink integration
            log.info("Publishing event={} attempt={} payload={}", eventType, attempt + 1, payload);
        } catch (Exception ex) {
            log.warn("Event publish failed for {} attempt {}: {}", eventType, attempt + 1, ex.getMessage());
            if (attempt + 1 < maxAttempts) {
                sleep(Duration.ofSeconds((long) Math.pow(2, attempt + 1)));
                dispatch(eventType, payload, attempt + 1);
            } else {
                log.error("Event publish permanently failed for {} after {} attempts", eventType, maxAttempts);
                eventPublishMonitor.recordEventFailure(eventType, maxAttempts);
            }
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}

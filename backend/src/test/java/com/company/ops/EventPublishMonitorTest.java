package com.company.ops;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EventPublishMonitorTest {

    @Test
    void logsWithoutThrowing() {
        EventPublishMonitor monitor = new EventPublishMonitor();
        assertDoesNotThrow(() -> monitor.recordEventFailure("TestEvent", 3));
        assertDoesNotThrow(() -> monitor.recordDeductionFailure(UUID.randomUUID(), "integration down"));
    }
}

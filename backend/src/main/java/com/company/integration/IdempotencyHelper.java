package com.company.integration;

import java.util.UUID;
import org.springframework.http.HttpHeaders;

public final class IdempotencyHelper {

    private IdempotencyHelper() {
    }

    public static void apply(HttpHeaders headers, UUID requestId) {
        if (headers == null || requestId == null) {
            return;
        }
        headers.add("Idempotency-Key", requestId.toString());
        headers.add("X-Request-Id", requestId.toString());
    }
}

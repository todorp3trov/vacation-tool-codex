package com.company.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceSessionCacheTest {

    private final BalanceSessionCache cache = new BalanceSessionCache();

    @Test
    void storesAndRetrievesCachedBalance() {
        MockHttpSession session = new MockHttpSession();
        UUID userId = UUID.randomUUID();

        cache.store(session, userId, BigDecimal.valueOf(12));

        BalanceSessionCache.BalanceSnapshot snapshot = cache.getSnapshot(session, userId);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.balance()).isEqualByComparingTo("12");
        assertThat(snapshot.unavailable()).isFalse();
    }

    @Test
    void returnsNullForDifferentUser() {
        MockHttpSession session = new MockHttpSession();
        cache.store(session, UUID.randomUUID(), BigDecimal.ONE);

        BalanceSessionCache.BalanceSnapshot snapshot = cache.getSnapshot(session, UUID.randomUUID());

        assertThat(snapshot).isNull();
    }
}

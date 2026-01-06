package com.company.service;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class BalanceSessionCache {
    public static final String ATTR_BALANCE_CACHE = "balanceCache";

    public BalanceSnapshot getSnapshot(HttpSession session, UUID userId) {
        Object attr = session.getAttribute(ATTR_BALANCE_CACHE);
        if (attr instanceof BalanceSnapshot snapshot && snapshot.userId().equals(userId)) {
            return snapshot;
        }
        return null;
    }

    public void store(HttpSession session, UUID userId, BigDecimal balance) {
        session.setAttribute(ATTR_BALANCE_CACHE, new BalanceSnapshot(userId, balance, false));
    }

    public void storeUnavailable(HttpSession session, UUID userId) {
        session.setAttribute(ATTR_BALANCE_CACHE, new BalanceSnapshot(userId, null, true));
    }

    public record BalanceSnapshot(UUID userId, BigDecimal balance, boolean unavailable) {
    }
}

package com.company.service;

import java.util.Collection;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RbacCacheInvalidator {
    private static final Logger log = LoggerFactory.getLogger(RbacCacheInvalidator.class);

    public void invalidateForUser(UUID userId) {
        if (userId != null) {
            log.info("Invalidating RBAC cache for user {}", userId);
        }
    }

    public void invalidateForTeams(Collection<UUID> teamIds) {
        if (teamIds != null && !teamIds.isEmpty()) {
            log.info("Invalidating team visibility caches for teams {}", teamIds);
        }
    }
}

package com.company.service;

import com.company.model.AuditActionType;
import com.company.model.AuditLog;
import com.company.repos.AuditLogRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void recordDashboardView(UUID actorId, boolean balanceUnavailable) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.EMPLOYEE_DASHBOARD_VIEW);
        logEntry.setEntityType("DASHBOARD");
        logEntry.setEntityId("EMPLOYEE");
        logEntry.setDetails("balance_unavailable=" + balanceUnavailable);
        try {
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for dashboard view: {}", ex.getMessage());
        }
    }
}

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

    public void recordSubmission(UUID actorId, UUID requestId, String requestCode, int numberOfDays) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.VACATION_REQUEST_SUBMITTED);
        logEntry.setEntityType("VACATION_REQUEST");
        logEntry.setEntityId(requestId != null ? requestId.toString() : requestCode);
        logEntry.setDetails("days=" + numberOfDays + ", code=" + requestCode);
        persistSafely(logEntry, "vacation submission");
    }

    public void recordSubmissionBlocked(UUID actorId, String reason) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.VACATION_REQUEST_SUBMISSION_BLOCKED);
        logEntry.setEntityType("VACATION_REQUEST");
        logEntry.setEntityId("SUBMISSION");
        logEntry.setDetails(reason);
        persistSafely(logEntry, "submission blocked");
    }

    public void recordDecision(UUID actorId, UUID requestId, String requestCode, boolean approved, String note) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(approved ? AuditActionType.VACATION_REQUEST_APPROVED : AuditActionType.VACATION_REQUEST_DENIED);
        logEntry.setEntityType("VACATION_REQUEST");
        logEntry.setEntityId(requestId != null ? requestId.toString() : requestCode);
        logEntry.setDetails(note);
        persistSafely(logEntry, approved ? "approval" : "denial");
    }

    private void persistSafely(AuditLog logEntry, String context) {
        try {
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for {}: {}", context, ex.getMessage());
        }
    }
}

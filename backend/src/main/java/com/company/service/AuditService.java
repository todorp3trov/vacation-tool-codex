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

    public void recordProcessingAttempt(UUID actorId, UUID requestId, String requestCode) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.HR_PROCESSING_ATTEMPT);
        logEntry.setEntityType("VACATION_REQUEST");
        logEntry.setEntityId(requestId != null ? requestId.toString() : requestCode);
        logEntry.setDetails("processing_attempt");
        persistSafely(logEntry, "hr processing attempt");
    }

    public void recordProcessingSuccess(UUID actorId, UUID requestId, String requestCode) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.HR_PROCESSING_SUCCESS);
        logEntry.setEntityType("VACATION_REQUEST");
        logEntry.setEntityId(requestId != null ? requestId.toString() : requestCode);
        logEntry.setDetails("processed");
        persistSafely(logEntry, "hr processing success");
    }

    public void recordProcessingFailure(UUID actorId, UUID requestId, String requestCode, String reason) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.HR_PROCESSING_FAILED);
        logEntry.setEntityType("VACATION_REQUEST");
        logEntry.setEntityId(requestId != null ? requestId.toString() : requestCode);
        logEntry.setDetails(reason);
        persistSafely(logEntry, "hr processing failure");
    }

    public void recordUserInvite(UUID actorId, UUID userId, String username) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_USER_INVITED);
        logEntry.setEntityType("USER");
        logEntry.setEntityId(userId != null ? userId.toString() : username);
        logEntry.setDetails(username);
        persistSafely(logEntry, "user invite");
    }

    public void recordUserActivation(UUID actorId, UUID userId, String username) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_USER_ACTIVATED);
        logEntry.setEntityType("USER");
        logEntry.setEntityId(userId != null ? userId.toString() : username);
        logEntry.setDetails("activated");
        persistSafely(logEntry, "user activation");
    }

    public void recordUserDisabled(UUID actorId, UUID userId, String username) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_USER_DISABLED);
        logEntry.setEntityType("USER");
        logEntry.setEntityId(userId != null ? userId.toString() : username);
        logEntry.setDetails("disabled");
        persistSafely(logEntry, "user disabled");
    }

    public void recordUserUpdate(UUID actorId, UUID userId, String username) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_USER_UPDATED);
        logEntry.setEntityType("USER");
        logEntry.setEntityId(userId != null ? userId.toString() : username);
        logEntry.setDetails("updated");
        persistSafely(logEntry, "user updated");
    }

    public void recordTeamCreated(UUID actorId, UUID teamId, String name) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_TEAM_CREATED);
        logEntry.setEntityType("TEAM");
        logEntry.setEntityId(teamId != null ? teamId.toString() : name);
        logEntry.setDetails(name);
        persistSafely(logEntry, "team created");
    }

    public void recordTeamArchived(UUID actorId, UUID teamId, String name) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_TEAM_ARCHIVED);
        logEntry.setEntityType("TEAM");
        logEntry.setEntityId(teamId != null ? teamId.toString() : name);
        logEntry.setDetails("archived");
        persistSafely(logEntry, "team archived");
    }

    public void recordTeamUpdated(UUID actorId, UUID teamId, String name) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_TEAM_UPDATED);
        logEntry.setEntityType("TEAM");
        logEntry.setEntityId(teamId != null ? teamId.toString() : name);
        logEntry.setDetails("updated");
        persistSafely(logEntry, "team updated");
    }

    public void recordTeamMembershipAdded(UUID actorId, UUID userId, UUID teamId) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_TEAM_MEMBERSHIP_ADDED);
        logEntry.setEntityType("TEAM_MEMBERSHIP");
        logEntry.setEntityId(teamId + ":" + userId);
        logEntry.setDetails("added");
        persistSafely(logEntry, "team membership added");
    }

    public void recordTeamMembershipRemoved(UUID actorId, UUID userId, UUID teamId) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.ADMIN_TEAM_MEMBERSHIP_REMOVED);
        logEntry.setEntityType("TEAM_MEMBERSHIP");
        logEntry.setEntityId(teamId + ":" + userId);
        logEntry.setDetails("removed");
        persistSafely(logEntry, "team membership removed");
    }

    public void recordIntegrationConfigured(UUID actorId, UUID configId, String type, String endpoint) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.INTEGRATION_CONFIGURED);
        logEntry.setEntityType("INTEGRATION_CONFIG");
        logEntry.setEntityId(configId != null ? configId.toString() : type);
        logEntry.setDetails("type=" + type + " endpoint=" + endpoint);
        persistSafely(logEntry, "integration configured");
    }

    public void recordIntegrationDisabled(UUID actorId, UUID configId, String type) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.INTEGRATION_DISABLED);
        logEntry.setEntityType("INTEGRATION_CONFIG");
        logEntry.setEntityId(configId != null ? configId.toString() : type);
        logEntry.setDetails("disabled type=" + type);
        persistSafely(logEntry, "integration disabled");
    }

    public void recordHolidayImport(UUID actorId, int year, int imported, int skipped, String outcome) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.HOLIDAY_IMPORTED);
        logEntry.setEntityType("HOLIDAY_IMPORT");
        logEntry.setEntityId(Integer.toString(year));
        logEntry.setDetails("imported=" + imported + ", skipped=" + skipped + ", outcome=" + outcome);
        persistSafely(logEntry, "holiday import");
    }

    public void recordHolidayDeprecation(UUID actorId, UUID holidayId, String reason) {
        AuditLog logEntry = new AuditLog();
        logEntry.setActorId(actorId);
        logEntry.setActionType(AuditActionType.HOLIDAY_DEPRECATED);
        logEntry.setEntityType("HOLIDAY");
        logEntry.setEntityId(holidayId != null ? holidayId.toString() : null);
        logEntry.setDetails(reason);
        persistSafely(logEntry, "holiday deprecation");
    }

    private void persistSafely(AuditLog logEntry, String context) {
        try {
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.warn("Failed to persist audit log for {}: {}", context, ex.getMessage());
        }
    }
}

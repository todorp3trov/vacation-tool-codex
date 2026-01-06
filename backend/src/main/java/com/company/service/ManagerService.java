package com.company.service;

import com.company.dto.DashboardResponse;
import com.company.dto.ManagerCalendarResponse;
import com.company.dto.ManagerPendingItem;
import com.company.dto.ManagerRequestDetail;
import com.company.dto.TentativeBalanceDto;
import com.company.integration.EventPublisher;
import com.company.integration.VacationBalanceClient;
import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import com.company.model.TeamMembershipStatus;
import com.company.model.TeamStatus;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import com.company.repos.HolidayRepository;
import com.company.repos.TeamMembershipRepository;
import com.company.repos.VacationRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerService {

    private final VacationRequestRepository vacationRequestRepository;
    private final HolidayRepository holidayRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final BalanceComputationService balanceComputationService;
    private final VacationBalanceClient vacationBalanceClient;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    public ManagerService(VacationRequestRepository vacationRequestRepository,
                          HolidayRepository holidayRepository,
                          TeamMembershipRepository teamMembershipRepository,
                          BalanceComputationService balanceComputationService,
                          VacationBalanceClient vacationBalanceClient,
                          EventPublisher eventPublisher,
                          AuditService auditService) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.holidayRepository = holidayRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.balanceComputationService = balanceComputationService;
        this.vacationBalanceClient = vacationBalanceClient;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    public List<ManagerPendingItem> loadPendingRequests() {
        List<VacationRequest> pending = vacationRequestRepository.findByStatusWithUser(VacationRequestStatus.PENDING);
        return pending.stream()
                .map(this::toPendingItem)
                .toList();
    }

    public ManagerCalendarResponse loadCalendar(UUID managerId, LocalDate startDate, LocalDate endDate) {
        List<UUID> teamIds = teamMembershipRepository.findActiveTeamIdsForUser(
                managerId,
                TeamMembershipStatus.ACTIVE,
                TeamStatus.ACTIVE
        );
        if (teamIds.isEmpty()) {
            return new ManagerCalendarResponse(List.of(), List.of());
        }
        List<UUID> teamUserIds = teamMembershipRepository.findActiveUserIdsForTeams(
                teamIds,
                TeamMembershipStatus.ACTIVE,
                TeamStatus.ACTIVE
        );
        if (teamUserIds.isEmpty()) {
            return new ManagerCalendarResponse(List.of(), List.of());
        }

        List<VacationRequest> vacations = vacationRequestRepository.findTeamVacations(
                teamUserIds,
                List.of(VacationRequestStatus.PENDING, VacationRequestStatus.APPROVED),
                startDate,
                endDate
        );
        List<DashboardResponse.VacationItem> vacationItems = vacations.stream()
                .map(vr -> new DashboardResponse.VacationItem(
                        vr.getId(),
                        vr.getUser().getDisplayName(),
                        vr.getStartDate(),
                        vr.getEndDate(),
                        vr.getStatus().name(),
                        vr.getUser().getId().equals(managerId)
                ))
                .toList();
        List<Holiday> holidays = holidayRepository.findForRange(HolidayStatus.IMPORTED, startDate, endDate);
        List<DashboardResponse.HolidayItem> holidayItems = holidays.stream()
                .map(h -> new DashboardResponse.HolidayItem(h.getId(), h.getDate(), h.getName()))
                .toList();
        return new ManagerCalendarResponse(vacationItems, holidayItems);
    }

    public ManagerRequestDetail loadRequestDetail(UUID requestId) {
        Optional<VacationRequest> requestOptional = vacationRequestRepository.findByIdWithUser(requestId);
        if (requestOptional.isEmpty()) {
            return null;
        }
        VacationRequest request = requestOptional.get();
        ManagerPendingItem view = toPendingItem(request);
        List<Holiday> holidays = holidayRepository.findForRange(HolidayStatus.IMPORTED, request.getStartDate(), request.getEndDate());
        List<DashboardResponse.HolidayItem> holidayItems = holidays.stream()
                .map(h -> new DashboardResponse.HolidayItem(h.getId(), h.getDate(), h.getName()))
                .toList();
        List<DashboardResponse.VacationItem> overlaps = loadOverlaps(request);
        return new ManagerRequestDetail(view, holidayItems, overlaps);
    }

    @Transactional
    public ManagerDecisionResult approve(UUID managerId, UUID requestId, String note) {
        Optional<VacationRequest> requestOptional = vacationRequestRepository.findByIdWithUser(requestId);
        if (requestOptional.isEmpty()) {
            return ManagerDecisionResult.missing();
        }
        VacationRequest request = requestOptional.get();
        if (request.getStatus() != VacationRequestStatus.PENDING) {
            return ManagerDecisionResult.failure("invalid_state", "Only pending requests can be approved");
        }
        VacationBalanceClient.BalanceResult balanceResult = vacationBalanceClient.fetchBalance(request.getUser().getId());
        if (balanceResult.unavailable()) {
            eventPublisher.publishImmediate("ExternalBalanceSystemUnavailable", Map.of(
                    "employeeId", request.getUser().getId().toString(),
                    "requestId", request.getId().toString()
            ));
            auditService.recordSubmissionBlocked(managerId, balanceResult.reason());
            return ManagerDecisionResult.externalUnavailable(balanceResult.reason());
        }
        BigDecimal official = balanceResult.balance();
        if (BigDecimal.valueOf(request.getNumberOfDays()).compareTo(official) > 0) {
            return ManagerDecisionResult.failure("insufficient_balance", "Requested days exceed remaining balance");
        }

        request.setStatus(VacationRequestStatus.APPROVED);
        if (note != null && !note.isBlank()) {
            request.setManagerNotes(note);
        }
        VacationRequest saved = vacationRequestRepository.save(request);
        eventPublisher.publishPostCommit("VacationApproved", Map.of(
                "requestId", saved.getId().toString(),
                "requestCode", saved.getRequestCode(),
                "employeeId", saved.getUser().getId().toString(),
                "startDate", saved.getStartDate().toString(),
                "endDate", saved.getEndDate().toString(),
                "requestedDays", saved.getNumberOfDays()
        ));
        auditService.recordDecision(managerId, saved.getId(), saved.getRequestCode(), true, note);
        return ManagerDecisionResult.success(saved);
    }

    @Transactional
    public ManagerDecisionResult deny(UUID managerId, UUID requestId, String note) {
        Optional<VacationRequest> requestOptional = vacationRequestRepository.findByIdWithUser(requestId);
        if (requestOptional.isEmpty()) {
            return ManagerDecisionResult.missing();
        }
        VacationRequest request = requestOptional.get();
        if (request.getStatus() != VacationRequestStatus.PENDING) {
            return ManagerDecisionResult.failure("invalid_state", "Only pending requests can be denied");
        }
        request.setStatus(VacationRequestStatus.DENIED);
        if (note != null && !note.isBlank()) {
            request.setManagerNotes(note);
        }
        VacationRequest saved = vacationRequestRepository.save(request);
        eventPublisher.publishPostCommit("VacationDenied", Map.of(
                "requestId", saved.getId().toString(),
                "requestCode", saved.getRequestCode(),
                "employeeId", saved.getUser().getId().toString(),
                "startDate", saved.getStartDate().toString(),
                "endDate", saved.getEndDate().toString(),
                "requestedDays", saved.getNumberOfDays()
        ));
        auditService.recordDecision(managerId, saved.getId(), saved.getRequestCode(), false, note);
        return ManagerDecisionResult.success(saved);
    }

    private ManagerPendingItem toPendingItem(VacationRequest request) {
        TentativeBalanceDto balance = balanceComputationService.computeForManagerView(request.getUser().getId());
        return new ManagerPendingItem(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getDisplayName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNumberOfDays(),
                request.getStatus().name(),
                balance
        );
    }

    private List<DashboardResponse.VacationItem> loadOverlaps(VacationRequest request) {
        List<UUID> teamIds = teamMembershipRepository.findActiveTeamIdsForUser(
                request.getUser().getId(),
                TeamMembershipStatus.ACTIVE,
                TeamStatus.ACTIVE
        );
        if (teamIds.isEmpty()) {
            return List.of();
        }
        List<UUID> teamUserIds = teamMembershipRepository.findActiveUserIdsForTeams(
                teamIds,
                TeamMembershipStatus.ACTIVE,
                TeamStatus.ACTIVE
        );
        if (teamUserIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> dedupedUserIds = Set.copyOf(teamUserIds);
        Collection<UUID> targetUserIds = dedupedUserIds;
        List<VacationRequest> overlaps = vacationRequestRepository.findTeamVacations(
                targetUserIds,
                List.of(VacationRequestStatus.PENDING, VacationRequestStatus.APPROVED),
                request.getStartDate(),
                request.getEndDate()
        );
        return overlaps.stream()
                .filter(vr -> !vr.getId().equals(request.getId()))
                .map(vr -> new DashboardResponse.VacationItem(
                        vr.getId(),
                        vr.getUser().getDisplayName(),
                        vr.getStartDate(),
                        vr.getEndDate(),
                        vr.getStatus().name(),
                        vr.getUser().getId().equals(request.getUser().getId())
                ))
                .toList();
    }
}

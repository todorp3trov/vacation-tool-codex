package com.company.service;

import com.company.dto.DashboardResponse;
import com.company.dto.TentativeBalanceDto;
import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import com.company.model.TeamMembershipStatus;
import com.company.model.TeamStatus;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import com.company.repos.HolidayRepository;
import com.company.repos.TeamMembershipRepository;
import com.company.repos.VacationRequestRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final VacationRequestRepository vacationRequestRepository;
    private final HolidayRepository holidayRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final BalanceComputationService balanceComputationService;
    private final AuditService auditService;

    public DashboardService(VacationRequestRepository vacationRequestRepository,
                            HolidayRepository holidayRepository,
                            TeamMembershipRepository teamMembershipRepository,
                            BalanceComputationService balanceComputationService,
                            AuditService auditService) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.holidayRepository = holidayRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.balanceComputationService = balanceComputationService;
        this.auditService = auditService;
    }

    public DashboardResponse loadDashboard(UUID userId, HttpSession session, LocalDate startDate, LocalDate endDate) {
        TentativeBalanceDto balanceResult = balanceComputationService.computeForUser(userId, session);
        List<VacationRequest> myVacations = vacationRequestRepository.findOverlappingForUser(userId, startDate, endDate);
        List<VacationRequest> teammateVacations = loadTeamVacations(userId, startDate, endDate);
        List<Holiday> holidays = holidayRepository.findForRange(HolidayStatus.IMPORTED, startDate, endDate);

        DashboardResponse response = new DashboardResponse(
                toBalanceSummary(balanceResult),
                myVacations.stream().map(v -> toVacationItem(v, true)).toList(),
                teammateVacations.stream().map(v -> toVacationItem(v, false)).toList(),
                holidays.stream().map(this::toHolidayItem).toList()
        );

        auditService.recordDashboardView(userId, balanceResult.unavailable());
        return response;
    }

    private List<VacationRequest> loadTeamVacations(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<UUID> teamIds = teamMembershipRepository.findActiveTeamIdsForUser(
                userId,
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
        Set<UUID> dedupedUserIds = new HashSet<>(teamUserIds);
        dedupedUserIds.remove(userId);
        if (dedupedUserIds.isEmpty()) {
            return List.of();
        }
        return vacationRequestRepository.findTeamVacations(
                dedupedUserIds,
                List.of(VacationRequestStatus.PENDING, VacationRequestStatus.APPROVED),
                startDate,
                endDate
        );
    }

    private DashboardResponse.BalanceSummary toBalanceSummary(TentativeBalanceDto balanceResult) {
        return new DashboardResponse.BalanceSummary(
                balanceResult.officialBalance(),
                balanceResult.tentativeBalance(),
                balanceResult.unavailable(),
                balanceResult.message()
        );
    }

    private DashboardResponse.VacationItem toVacationItem(VacationRequest request, boolean mine) {
        return new DashboardResponse.VacationItem(
                request.getId(),
                request.getUser().getDisplayName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus().name(),
                mine
        );
    }

    private DashboardResponse.HolidayItem toHolidayItem(Holiday holiday) {
        return new DashboardResponse.HolidayItem(holiday.getId(), holiday.getDate(), holiday.getName());
    }
}

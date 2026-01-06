package com.company.service;

import com.company.dto.DashboardResponse;
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
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final VacationBalanceClient vacationBalanceClient;
    private final BalanceSessionCache balanceSessionCache;
    private final AuditService auditService;

    public DashboardService(VacationRequestRepository vacationRequestRepository,
                            HolidayRepository holidayRepository,
                            TeamMembershipRepository teamMembershipRepository,
                            VacationBalanceClient vacationBalanceClient,
                            BalanceSessionCache balanceSessionCache,
                            AuditService auditService) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.holidayRepository = holidayRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.vacationBalanceClient = vacationBalanceClient;
        this.balanceSessionCache = balanceSessionCache;
        this.auditService = auditService;
    }

    public DashboardResponse loadDashboard(UUID userId, HttpSession session, LocalDate startDate, LocalDate endDate) {
        BalanceSessionCache.BalanceSnapshot cached = balanceSessionCache.getSnapshot(session, userId);
        VacationBalanceClient.BalanceResult balanceResult;
        if (cached != null) {
            balanceResult = cached.unavailable()
                    ? VacationBalanceClient.BalanceResult.unavailable("cached unavailable")
                    : VacationBalanceClient.BalanceResult.available(cached.balance());
        } else {
            balanceResult = vacationBalanceClient.fetchBalance(userId);
            if (balanceResult.unavailable()) {
                balanceSessionCache.storeUnavailable(session, userId);
            } else {
                balanceSessionCache.store(session, userId, balanceResult.balance());
            }
        }

        List<VacationRequest> myVacations = vacationRequestRepository.findOverlappingForUser(userId, startDate, endDate);
        List<VacationRequest> teammateVacations = loadTeamVacations(userId, startDate, endDate);
        List<Holiday> holidays = holidayRepository.findForRange(HolidayStatus.IMPORTED, startDate, endDate);

        DashboardResponse response = new DashboardResponse(
                toBalanceSummary(balanceResult, myVacations),
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

    private DashboardResponse.BalanceSummary toBalanceSummary(VacationBalanceClient.BalanceResult balanceResult,
                                                              List<VacationRequest> myVacations) {
        BigDecimal official = balanceResult.unavailable() ? null : balanceResult.balance();
        BigDecimal tentative = null;
        if (official != null) {
            long pendingDays = myVacations.stream()
                    .filter(v -> v.getStatus() == VacationRequestStatus.PENDING)
                    .mapToLong(this::dayCount)
                    .sum();
            tentative = official.subtract(BigDecimal.valueOf(pendingDays));
            if (tentative.compareTo(BigDecimal.ZERO) < 0) {
                tentative = BigDecimal.ZERO;
            }
        }
        return new DashboardResponse.BalanceSummary(official, tentative, balanceResult.unavailable(), balanceResult.reason());
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

    private long dayCount(VacationRequest request) {
        return ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
    }
}

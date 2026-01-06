package com.company.service;

import com.company.dto.DashboardResponse;
import com.company.dto.TentativeBalanceDto;
import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import com.company.model.User;
import com.company.model.VacationRequest;
import com.company.model.VacationRequestStatus;
import com.company.repos.HolidayRepository;
import com.company.repos.TeamMembershipRepository;
import com.company.repos.VacationRequestRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VacationRequestRepository vacationRequestRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private TeamMembershipRepository teamMembershipRepository;

    @Mock
    private BalanceComputationService balanceComputationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DashboardService dashboardService;

    private final UUID userId = UUID.randomUUID();
    private final LocalDate start = LocalDate.of(2024, 6, 1);
    private final LocalDate end = LocalDate.of(2024, 6, 30);
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Test
    void computesTentativeBalanceAndMapsVacations() {
        VacationRequest pending = buildVacation(userId, "Alice Employee", LocalDate.of(2024, 6, 10),
                LocalDate.of(2024, 6, 11), VacationRequestStatus.PENDING);
        User teammate = new User();
        teammate.setId(UUID.randomUUID());
        teammate.setDisplayName("Bob Teammate");
        VacationRequest approvedTeam = buildVacation(teammate.getId(), teammate.getDisplayName(),
                LocalDate.of(2024, 6, 12), LocalDate.of(2024, 6, 12), VacationRequestStatus.APPROVED);
        Holiday holiday = new Holiday();
        holiday.setDate(LocalDate.of(2024, 6, 15));
        holiday.setName("Mid-June Holiday");
        holiday.setStatus(HolidayStatus.IMPORTED);

        when(balanceComputationService.computeForUser(userId, session)).thenReturn(
                new TentativeBalanceDto(BigDecimal.valueOf(15), BigDecimal.valueOf(13), false, null)
        );
        when(vacationRequestRepository.findOverlappingForUser(userId, start, end)).thenReturn(List.of(pending));
        when(teamMembershipRepository.findActiveTeamIdsForUser(any(), any(), any()))
                .thenReturn(List.of(UUID.randomUUID()));
        when(teamMembershipRepository.findActiveUserIdsForTeams(any(), any(), any()))
                .thenReturn(List.of(teammate.getId(), userId));
        when(vacationRequestRepository.findTeamVacations(any(), any(), any(), any()))
                .thenReturn(List.of(approvedTeam));
        when(holidayRepository.findForRange(HolidayStatus.IMPORTED, start, end)).thenReturn(List.of(holiday));

        DashboardResponse response = dashboardService.loadDashboard(userId, session, start, end);

        assertThat(response.balance().officialBalance()).isEqualByComparingTo("15");
        assertThat(response.balance().tentativeBalance()).isEqualByComparingTo("13");
        assertThat(response.myVacations()).hasSize(1);
        assertThat(response.teammateVacations()).extracting(DashboardResponse.VacationItem::employeeName)
                .contains("Bob Teammate");
        assertThat(response.holidays()).hasSize(1);

        verify(auditService).recordDashboardView(userId, false);
    }

    private VacationRequest buildVacation(UUID userId, String displayName, LocalDate startDate,
                                          LocalDate endDate, VacationRequestStatus status) {
        User user = new User();
        user.setId(userId);
        user.setDisplayName(displayName);
        VacationRequest request = new VacationRequest();
        request.setUser(user);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setStatus(status);
        return request;
    }
}

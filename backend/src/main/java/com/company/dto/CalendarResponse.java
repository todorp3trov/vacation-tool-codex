package com.company.dto;

import java.util.List;

public record CalendarResponse(List<String> roles,
                               DashboardResponse.BalanceSummary balance,
                               List<DashboardResponse.VacationItem> myVacations,
                               List<DashboardResponse.VacationItem> teammateVacations,
                               List<DashboardResponse.HolidayItem> holidays,
                               List<ManagerPendingItem> managerPending) {
}

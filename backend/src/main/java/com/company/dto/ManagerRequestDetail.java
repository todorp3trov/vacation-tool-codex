package com.company.dto;

import java.util.List;

public record ManagerRequestDetail(ManagerPendingItem request,
                                   List<DashboardResponse.HolidayItem> holidays,
                                   List<DashboardResponse.VacationItem> overlaps) {
}

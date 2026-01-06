package com.company.dto;

import java.util.List;

public record ManagerCalendarResponse(List<DashboardResponse.VacationItem> vacations,
                                      List<DashboardResponse.HolidayItem> holidays) {
}

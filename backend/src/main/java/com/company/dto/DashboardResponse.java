package com.company.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        BalanceSummary balance,
        List<VacationItem> myVacations,
        List<VacationItem> teammateVacations,
        List<HolidayItem> holidays
) {
    public record BalanceSummary(BigDecimal officialBalance,
                                 BigDecimal tentativeBalance,
                                 boolean unavailable,
                                 String message) {
    }

    public record VacationItem(UUID id,
                               String employeeName,
                               LocalDate startDate,
                               LocalDate endDate,
                               String status,
                               boolean mine) {
    }

    public record HolidayItem(UUID id, LocalDate date, String name) {
    }
}

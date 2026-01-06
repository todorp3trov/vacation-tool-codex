package com.company.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ManagerPendingItem(UUID requestId,
                                 UUID employeeId,
                                 String employeeName,
                                 LocalDate startDate,
                                 LocalDate endDate,
                                 int numberOfDays,
                                 String status,
                                 TentativeBalanceDto balance) {
}

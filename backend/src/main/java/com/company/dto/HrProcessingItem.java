package com.company.dto;

import java.time.LocalDate;
import java.util.UUID;

public record HrProcessingItem(UUID requestId,
                               UUID employeeId,
                               String employeeName,
                               LocalDate startDate,
                               LocalDate endDate,
                               int numberOfDays,
                               String status,
                               String requestCode,
                               String managerNotes,
                               String hrNotes) {
}

package com.company.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record VacationSubmissionRequest(
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
}

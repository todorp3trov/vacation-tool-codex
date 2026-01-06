package com.company.service;

import com.company.model.Holiday;
import com.company.model.HolidayStatus;
import com.company.repos.HolidayRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DayCountService {

    private final HolidayRepository holidayRepository;

    public DayCountService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public long computeNumberOfDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        List<LocalDate> holidayDates = holidayRepository.findForRange(HolidayStatus.IMPORTED, startDate, endDate)
                .stream()
                .map(Holiday::getDate)
                .distinct()
                .toList();
        long nonWorkingHolidays = holidayDates.size();
        long computed = inclusiveDays - nonWorkingHolidays;
        return Math.max(computed, 0);
    }
}

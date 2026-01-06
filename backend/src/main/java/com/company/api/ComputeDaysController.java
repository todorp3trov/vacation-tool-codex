package com.company.api;

import com.company.service.DayCountService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ComputeDaysController {

    private final DayCountService dayCountService;

    public ComputeDaysController(DayCountService dayCountService) {
        this.dayCountService = dayCountService;
    }

    @GetMapping("/compute-days")
    public ResponseEntity<?> computeDays(@RequestParam("start")
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                         @RequestParam("end")
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start == null || end == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "start and end are required"));
        }
        if (end.isBefore(start)) {
            return ResponseEntity.badRequest().body(Map.of("error", "end must be on or after start"));
        }

        long days = dayCountService.computeNumberOfDays(start, end);
        return ResponseEntity.ok(Map.of("number_of_days", days));
    }
}

package com.company.api;

import com.company.dto.CalendarResponse;
import com.company.dto.DashboardResponse;
import com.company.dto.ManagerPendingItem;
import com.company.service.DashboardService;
import com.company.service.ManagerService;
import com.company.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CalendarController {

    private final SessionService sessionService;
    private final DashboardService dashboardService;
    private final ManagerService managerService;

    public CalendarController(SessionService sessionService,
                              DashboardService dashboardService,
                              ManagerService managerService) {
        this.sessionService = sessionService;
        this.dashboardService = dashboardService;
        this.managerService = managerService;
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendar(@RequestParam(value = "start", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                         @RequestParam(value = "end", required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                         HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return unauthorized();
        }
        UUID userId = sessionService.getUserId(session);
        if (userId == null) {
            return unauthorized();
        }
        List<String> roles = sessionService.getRoles(session);

        LocalDate rangeStart = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate rangeEnd = end != null ? end : rangeStart.plusMonths(1).withDayOfMonth(rangeStart.plusMonths(1).lengthOfMonth());
        if (rangeEnd.isBefore(rangeStart)) {
            return ResponseEntity.badRequest().body(Map.of("error", "start must be on or before end"));
        }

        DashboardResponse base = dashboardService.loadDashboard(userId, session, rangeStart, rangeEnd);
        List<ManagerPendingItem> pending = roles.stream().anyMatch(r -> r.equalsIgnoreCase("MANAGER"))
                ? managerService.loadPendingRequests()
                : List.of();
        CalendarResponse response = new CalendarResponse(
                roles,
                base.balance(),
                base.myVacations(),
                base.teammateVacations(),
                base.holidays(),
                pending
        );
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
    }
}

package com.company.api;

import com.company.dto.ManagerCalendarResponse;
import com.company.dto.ManagerPendingItem;
import com.company.dto.ManagerRequestDetail;
import com.company.model.VacationRequest;
import com.company.service.ManagerDecisionResult;
import com.company.service.ManagerService;
import com.company.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    private final ManagerService managerService;
    private final SessionService sessionService;

    public ManagerController(ManagerService managerService, SessionService sessionService) {
        this.managerService = managerService;
        this.sessionService = sessionService;
    }

    @GetMapping("/pending")
    public ResponseEntity<?> pending(HttpServletRequest request) {
        if (!isAuthorized(request)) {
            return unauthorized();
        }
        return ResponseEntity.ok(managerService.loadPendingRequests());
    }

    @GetMapping("/request/{id}")
    public ResponseEntity<?> detail(@PathVariable("id") UUID requestId, HttpServletRequest request) {
        if (!isAuthorized(request)) {
            return unauthorized();
        }
        ManagerRequestDetail detail = managerService.loadRequestDetail(requestId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/calendar")
    public ResponseEntity<?> calendar(@RequestParam(value = "start", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                      @RequestParam(value = "end", required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                      HttpServletRequest servletRequest) {
        UUID managerId = actor(servletRequest);
        if (managerId == null) {
            return unauthorized();
        }
        LocalDate rangeStart = start != null ? start : LocalDate.now().withDayOfMonth(1);
        LocalDate rangeEnd = end != null ? end : rangeStart.plusMonths(1).withDayOfMonth(rangeStart.plusMonths(1).lengthOfMonth());
        if (rangeEnd.isBefore(rangeStart)) {
            return ResponseEntity.badRequest().body(Map.of("error", "start must be on or before end"));
        }
        ManagerCalendarResponse response = managerService.loadCalendar(managerId, rangeStart, rangeEnd);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable("id") UUID requestId,
                                     @RequestBody(required = false) ManagerDecisionRequest body,
                                     HttpServletRequest request) {
        UUID managerId = actor(request);
        if (managerId == null) {
            return unauthorized();
        }
        String note = body != null ? body.note() : null;
        ManagerDecisionResult result = managerService.approve(managerId, requestId, note);
        return mapDecisionResult(result);
    }

    @PostMapping("/request/{id}/deny")
    public ResponseEntity<?> deny(@PathVariable("id") UUID requestId,
                                  @RequestBody(required = false) ManagerDecisionRequest body,
                                  HttpServletRequest request) {
        UUID managerId = actor(request);
        if (managerId == null) {
            return unauthorized();
        }
        String note = body != null ? body.note() : null;
        ManagerDecisionResult result = managerService.deny(managerId, requestId, note);
        return mapDecisionResult(result);
    }

    private ResponseEntity<?> mapDecisionResult(ManagerDecisionResult result) {
        if (result.notFound()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        }
        if (result.externalUnavailable()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", result.error(),
                    "message", result.message()
            ));
        }
        if (!result.success()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", result.error(),
                    "message", result.message()
            ));
        }
        VacationRequest request = result.request();
        ManagerPendingItem view = new ManagerPendingItem(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getDisplayName(),
                request.getStartDate(),
                request.getEndDate(),
                request.getNumberOfDays(),
                request.getStatus().name(),
                null
        );
        return ResponseEntity.ok(view);
    }

    private boolean isAuthorized(HttpServletRequest servletRequest) {
        return actor(servletRequest) != null;
    }

    private UUID actor(HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            return null;
        }
        return sessionService.getUserId(session);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
    }
}

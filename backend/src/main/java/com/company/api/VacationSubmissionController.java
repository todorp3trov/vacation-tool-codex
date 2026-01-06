package com.company.api;

import com.company.model.VacationRequest;
import com.company.service.SessionService;
import com.company.service.VacationRequestService;
import com.company.service.VacationSubmissionResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vacation")
public class VacationSubmissionController {

    private final VacationRequestService vacationRequestService;
    private final SessionService sessionService;

    public VacationSubmissionController(VacationRequestService vacationRequestService,
                                        SessionService sessionService) {
        this.vacationRequestService = vacationRequestService;
        this.sessionService = sessionService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody VacationSubmissionRequest requestBody,
                                    HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            return unauthorized();
        }
        UUID userId = sessionService.getUserId(session);
        if (userId == null) {
            return unauthorized();
        }
        if (requestBody == null || requestBody.startDate() == null || requestBody.endDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "start_date_and_end_date_required"));
        }

        try {
            VacationSubmissionResult result = vacationRequestService.submit(
                    userId,
                    session,
                    requestBody.startDate(),
                    requestBody.endDate()
            );
            if (result.success()) {
                VacationRequest saved = result.request();
                return ResponseEntity.ok(Map.of(
                        "requestId", saved.getId(),
                        "requestCode", saved.getRequestCode(),
                        "status", saved.getStatus().name(),
                        "number_of_days", saved.getNumberOfDays()
                ));
            }
            if (result.externalUnavailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                        "error", result.error(),
                        "message", result.message()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "error", result.error(),
                    "message", result.message()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "validation_failed",
                    "message", ex.getMessage()
            ));
        }
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
    }
}

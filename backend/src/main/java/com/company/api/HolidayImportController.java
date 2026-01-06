package com.company.api;

import com.company.dto.AdminDtos.HolidayImportResponse;
import com.company.service.HolidayImportService;
import com.company.service.SessionService;
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
@RequestMapping("/api/admin/holidays")
public class HolidayImportController {

    private final HolidayImportService holidayImportService;
    private final SessionService sessionService;

    public HolidayImportController(HolidayImportService holidayImportService, SessionService sessionService) {
        this.holidayImportService = holidayImportService;
        this.sessionService = sessionService;
    }

    @PostMapping("/import")
    public ResponseEntity<?> importHolidays(@RequestBody ImportRequest request, HttpServletRequest servletRequest) {
        UUID actor = actor(servletRequest);
        if (actor == null) {
            return unauthorized();
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year_required"));
        }
        HolidayImportResponse response = holidayImportService.importForYear(actor, request.year());
        if ("failure".equalsIgnoreCase(response.outcome())) {
            HttpStatus status = "Year out of allowed range".equalsIgnoreCase(response.message())
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
        }
        return ResponseEntity.ok(response);
    }

    private UUID actor(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return sessionService.getUserId(session);
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
    }

    public record ImportRequest(int year) {
    }
}

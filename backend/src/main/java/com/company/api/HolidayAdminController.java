package com.company.api;

import com.company.dto.AdminDtos.HolidayAdminItem;
import com.company.dto.AdminDtos.HolidayDeprecateRequest;
import com.company.service.HolidayService;
import com.company.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
@RequestMapping("/api/admin/holidays")
public class HolidayAdminController {

    private final HolidayService holidayService;
    private final SessionService sessionService;

    public HolidayAdminController(HolidayService holidayService, SessionService sessionService) {
        this.holidayService = holidayService;
        this.sessionService = sessionService;
    }

    @GetMapping("/years")
    public ResponseEntity<?> years(HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        List<Integer> years = holidayService.listImportedYears();
        return ResponseEntity.ok(years);
    }

    @GetMapping("/imported")
    public ResponseEntity<?> imported(@RequestParam("year") int year, HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        List<HolidayAdminItem> items = holidayService.listImportedByYear(year);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/deprecate")
    public ResponseEntity<?> deprecate(@PathVariable("id") UUID id,
                                       @RequestBody(required = false) HolidayDeprecateRequest body,
                                       HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        HolidayAdminItem result = holidayService.deprecate(actor, id, body != null ? body.reason() : null);
        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        }
        return ResponseEntity.ok(result);
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
}

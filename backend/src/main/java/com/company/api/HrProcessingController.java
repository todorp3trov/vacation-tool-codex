package com.company.api;

import com.company.dto.HrProcessingDetail;
import com.company.dto.HrProcessingItem;
import com.company.service.HrProcessingResult;
import com.company.service.HrProcessingService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hr")
public class HrProcessingController {

    private final HrProcessingService hrProcessingService;
    private final SessionService sessionService;

    public HrProcessingController(HrProcessingService hrProcessingService, SessionService sessionService) {
        this.hrProcessingService = hrProcessingService;
        this.sessionService = sessionService;
    }

    @GetMapping("/queue")
    public ResponseEntity<?> queue(HttpServletRequest servletRequest) {
        UUID hrId = actor(servletRequest);
        if (hrId == null) {
            return unauthorized();
        }
        List<HrProcessingItem> items = hrProcessingService.loadQueue();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/request/{id}")
    public ResponseEntity<?> detail(@PathVariable("id") UUID requestId, HttpServletRequest servletRequest) {
        UUID hrId = actor(servletRequest);
        if (hrId == null) {
            return unauthorized();
        }
        HrProcessingDetail detail = hrProcessingService.loadDetail(requestId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
        }
        return ResponseEntity.ok(detail);
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(@RequestBody HrProcessRequest body, HttpServletRequest servletRequest) {
        UUID hrId = actor(servletRequest);
        if (hrId == null) {
            return unauthorized();
        }
        if (body == null || body.requestId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request_id_required"));
        }
        HrProcessingResult result = hrProcessingService.process(hrId, body.requestId(), body.hrNotes());
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
        HrProcessingItem item = new HrProcessingItem(
                result.request().getId(),
                result.request().getUser().getId(),
                result.request().getUser().getDisplayName(),
                result.request().getStartDate(),
                result.request().getEndDate(),
                result.request().getNumberOfDays(),
                result.request().getStatus().name(),
                result.request().getRequestCode(),
                result.request().getManagerNotes(),
                result.request().getHrNotes()
        );
        return ResponseEntity.ok(new HrProcessingDetail(item));
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

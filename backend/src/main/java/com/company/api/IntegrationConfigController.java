package com.company.api;

import com.company.dto.AdminDtos.IntegrationConfigRequest;
import com.company.service.IntegrationConfigService;
import com.company.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/integrations")
public class IntegrationConfigController {

    private final IntegrationConfigService integrationConfigService;
    private final SessionService sessionService;

    public IntegrationConfigController(IntegrationConfigService integrationConfigService,
                                       SessionService sessionService) {
        this.integrationConfigService = integrationConfigService;
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(integrationConfigService.listConfigs());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody IntegrationConfigRequest requestBody, HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(integrationConfigService.create(actor, requestBody));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") UUID id,
                                    @RequestBody IntegrationConfigRequest requestBody,
                                    HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        try {
            var result = integrationConfigService.update(actor, id, requestBody);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable("id") UUID id, HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        var result = integrationConfigService.disable(actor, id);
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

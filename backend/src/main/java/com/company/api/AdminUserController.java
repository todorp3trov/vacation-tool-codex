package com.company.api;

import com.company.dto.AdminDtos.AdminTeamRequest;
import com.company.dto.AdminDtos.AdminUserRequest;
import com.company.repos.RoleRepository;
import com.company.repos.UserRepository;
import com.company.service.AdminConfigurationService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminConfigurationService adminConfigurationService;
    private final SessionService sessionService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public AdminUserController(AdminConfigurationService adminConfigurationService,
                               SessionService sessionService,
                               RoleRepository roleRepository,
                               UserRepository userRepository) {
        this.adminConfigurationService = adminConfigurationService;
        this.sessionService = sessionService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(adminConfigurationService.listUsers());
    }

    @GetMapping("/roles")
    public ResponseEntity<?> listRoles(HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        List<String> codes = roleRepository.findAll().stream()
                .map(role -> role.getCode().toUpperCase())
                .sorted()
                .toList();
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/users/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam("username") String username, HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username_required"));
        }
        boolean available = !userRepository.existsByUsernameIgnoreCase(username);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody AdminUserRequest requestBody, HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminConfigurationService.createUser(actor, requestBody));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") UUID userId,
                                        @RequestBody AdminUserRequest requestBody,
                                        HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        try {
            var result = adminConfigurationService.updateUser(actor, userId, requestBody);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/teams")
    public ResponseEntity<?> listTeams(HttpServletRequest request) {
        if (actor(request) == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(adminConfigurationService.listTeams());
    }

    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody AdminTeamRequest requestBody, HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(adminConfigurationService.createTeam(actor, requestBody));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/teams/{id}")
    public ResponseEntity<?> updateTeam(@PathVariable("id") UUID teamId,
                                        @RequestBody AdminTeamRequest requestBody,
                                        HttpServletRequest request) {
        UUID actor = actor(request);
        if (actor == null) {
            return unauthorized();
        }
        try {
            var result = adminConfigurationService.updateTeam(actor, teamId, requestBody);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "not_found"));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
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

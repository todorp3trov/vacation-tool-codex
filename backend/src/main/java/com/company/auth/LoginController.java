package com.company.auth;

import com.company.model.User;
import com.company.model.UserStatus;
import com.company.repos.UserRepository;
import com.company.service.RoleService;
import com.company.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final RoleService roleService;

    public LoginController(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           SessionService sessionService,
                           RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.roleService = roleService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (request == null
                || !StringUtils.hasText(request.username())
                || !StringUtils.hasText(request.password())) {
            return error(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        Optional<User> userOptional = userRepository.findByUsernameIgnoreCase(request.username());
        if (userOptional.isEmpty()) {
            return unauthorized();
        }
        User user = userOptional.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            return unauthorized();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return unauthorized();
        }

        HttpSession session = sessionService.resetSession(servletRequest);
        sessionService.establishSession(session, user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        String homeRoute = roleService.resolveHomeRoute(user.getRoles());
        return ResponseEntity.ok(new LoginResponse(homeRoute));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionService.invalidateSession(request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionService.expiredCookie().toString())
                .build();
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}

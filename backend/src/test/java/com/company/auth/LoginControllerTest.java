package com.company.auth;

import com.company.model.Role;
import com.company.model.User;
import com.company.model.UserStatus;
import com.company.repos.UserRepository;
import com.company.service.RoleService;
import com.company.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final SessionService sessionService = new SessionService(new RoleService());
    private final RoleService roleService = new RoleService();
    private final LoginController controller = new LoginController(userRepository, passwordEncoder, sessionService, roleService);

    @Test
    void returnsBadRequestWhenMissingFields() {
        ResponseEntity<?> response = controller.login(new LoginRequest("", ""), new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void returnsUnauthorizedForUnknownUser() {
        when(userRepository.findByUsernameIgnoreCase("alice")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(new LoginRequest("alice", "pw"), new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logsInActiveUserAndReturnsHomeRoute() {
        User user = new User();
        user.setUsername("bob");
        user.setPasswordHash(passwordEncoder.encode("pw"));
        user.setStatus(UserStatus.ACTIVE);
        Role manager = new Role();
        manager.setCode("MANAGER");
        user.setRoles(Set.of(manager));
        when(userRepository.findByUsernameIgnoreCase("bob")).thenReturn(Optional.of(user));

        HttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<?> response = controller.login(new LoginRequest("bob", "pw"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
        LoginResponse body = (LoginResponse) response.getBody();
        assertThat(body.homeRoute()).isEqualTo("/manager");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getLastLoginAt()).isNotNull();
    }
}

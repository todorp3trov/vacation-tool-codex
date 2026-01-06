package com.company.service;

import com.company.dto.AdminDtos.AdminUserRequest;
import com.company.integration.EventPublisher;
import com.company.model.Role;
import com.company.model.User;
import com.company.repos.RoleRepository;
import com.company.repos.TeamMembershipRepository;
import com.company.repos.TeamRepository;
import com.company.repos.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConfigurationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMembershipRepository teamMembershipRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AuditService auditService;
    @Mock
    private RbacCacheInvalidator rbacCacheInvalidator;

    private RoleService roleService;
    private AdminConfigurationService service;
    private Role employeeRole;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
        service = new AdminConfigurationService(
                userRepository,
                roleRepository,
                teamRepository,
                teamMembershipRepository,
                passwordEncoder,
                roleService,
                eventPublisher,
                auditService,
                rbacCacheInvalidator
        );

        employeeRole = new Role();
        employeeRole.setId(UUID.randomUUID());
        employeeRole.setCode("EMPLOYEE");

        managerRole = new Role();
        managerRole.setId(UUID.randomUUID());
        managerRole.setCode("MANAGER");
    }

    @Test
    void roleChangeTriggersInvalidation() {
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("demo");
        user.setDisplayName("Demo User");
        user.setPasswordHash("hash");
        user.setRoles(Set.of(employeeRole));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByCode("MANAGER")).thenReturn(Optional.of(managerRole));
        when(roleRepository.findByCode("EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(teamMembershipRepository.findByUserId(userId)).thenReturn(List.of());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        AdminUserRequest update = new AdminUserRequest("demo", "Demo User", "", "ACTIVE", List.of("MANAGER"), List.of());
        service.updateUser(actorId, userId, update);

        verify(rbacCacheInvalidator).invalidateForUser(userId);
    }
}

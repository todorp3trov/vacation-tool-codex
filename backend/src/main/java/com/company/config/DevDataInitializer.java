package com.company.config;

import com.company.model.Role;
import com.company.model.User;
import com.company.model.UserStatus;
import com.company.repos.RoleRepository;
import com.company.repos.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DevDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-user:true}")
    private boolean seedDemoUser;

    @Value("${app.demo-user.username:demo}")
    private String demoUsername;

    @Value("${app.demo-user.password:password}")
    private String demoPassword;

    @Value("${app.demo-user.role:ADMIN}")
    private String demoRoleCode;

    public DevDataInitializer(UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedDemoUser || userRepository.existsByUsernameIgnoreCase(demoUsername)) {
            return;
        }
        Role role = loadOrCreateRole(demoRoleCode.toUpperCase());
        User user = new User();
        user.setUsername(demoUsername);
        user.setPasswordHash(passwordEncoder.encode(demoPassword));
        user.setDisplayName("Demo User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    private Role loadOrCreateRole(String code) {
        Optional<Role> existing = roleRepository.findByCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        Role role = new Role();
        role.setCode(code);
        role.setDescription(code + " auto-created");
        return roleRepository.save(role);
    }
}

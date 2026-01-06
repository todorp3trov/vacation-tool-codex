package com.company.service;

import com.company.model.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleServiceTest {

    private final RoleService roleService = new RoleService();

    @Test
    void resolvesHomeRouteByPriority() {
        Role admin = new Role();
        admin.setCode("ADMIN");
        Role manager = new Role();
        manager.setCode("MANAGER");

        String route = roleService.resolveHomeRoute(Set.of(manager, admin));

        assertThat(route).isEqualTo("/admin");
    }

    @Test
    void returnsSortedRoleCodes() {
        Role employee = new Role();
        employee.setCode("EMPLOYEE");
        Role hr = new Role();
        hr.setCode("HR");

        assertThat(roleService.toRoleCodes(Set.of(employee, hr)))
                .containsExactly("HR", "EMPLOYEE");
    }
}

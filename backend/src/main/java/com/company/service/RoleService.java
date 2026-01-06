package com.company.service;

import com.company.model.Role;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "HR", "MANAGER", "EMPLOYEE");

    public List<String> toRoleCodes(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getCode().toUpperCase())
                .distinct()
                .sorted(Comparator.comparingInt(this::priorityForCode))
                .toList();
    }

    public boolean hasRole(Set<Role> roles, String roleCode) {
        return toRoleCodes(roles).contains(roleCode.toUpperCase());
    }

    public String resolveHomeRoute(Set<Role> roles) {
        List<String> codes = new ArrayList<>(toRoleCodes(roles));
        if (codes.isEmpty()) {
            return "/";
        }
        return "/calendar";
    }

    public String resolveHomeRouteFromCodes(List<String> codes) {
        Set<Role> roles = codes.stream().map(code -> {
            Role role = new Role();
            role.setCode(code);
            return role;
        }).collect(Collectors.toSet());
        return resolveHomeRoute(roles);
    }

    private int priorityForCode(String code) {
        int index = ROLE_PRIORITY.indexOf(code);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}

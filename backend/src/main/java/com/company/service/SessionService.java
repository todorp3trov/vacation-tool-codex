package com.company.service;

import com.company.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLES = "roles";
    private static final String COOKIE_NAME = "SESSIONID";

    private final RoleService roleService;

    public SessionService(RoleService roleService) {
        this.roleService = roleService;
    }

    public HttpSession resetSession(HttpServletRequest request) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        return request.getSession(true);
    }

    public void establishSession(HttpSession session, User user) {
        session.setAttribute(ATTR_USER_ID, user.getId());
        session.setAttribute(ATTR_ROLES, roleService.toRoleCodes(user.getRoles()));
    }

    public List<String> getRoles(HttpSession session) {
        Object attr = session.getAttribute(ATTR_ROLES);
        if (attr instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return Collections.emptyList();
    }

    public UUID getUserId(HttpSession session) {
        Object attr = session.getAttribute(ATTR_USER_ID);
        if (attr instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }

    public void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public ResponseCookie expiredCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}

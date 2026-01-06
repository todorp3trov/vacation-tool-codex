package com.company.auth;

import java.util.List;
import java.util.Map;

/**
 * ContractSpecification centralizes key contract details used by tests to
 * validate parity between documentation and implementation.
 */
public final class ContractSpecification {

    private ContractSpecification() {
    }

    public static final String CONTRACT_VERSION = "1.0";
    public static final String DOCUMENT_ID = "auth-rbac-contract";

    public record Endpoint(String method, String path, List<Integer> successCodes, List<Integer> errorCodes) {
    }

    public record CookieRequirements(boolean httpOnly, boolean secure, String sameSite, String name) {
    }

    public record RolePriority(List<String> roles) {
    }

    public static Endpoint loginEndpoint() {
        return new Endpoint("POST", "/api/login", List.of(200), List.of(400, 401, 500));
    }

    public static Endpoint logoutEndpoint() {
        return new Endpoint("POST", "/api/logout", List.of(204), List.of(401, 500));
    }

    public static CookieRequirements sessionCookie() {
        return new CookieRequirements(true, true, "Lax", "SESSIONID");
    }

    public static RolePriority defaultRolePriority() {
        return new RolePriority(List.of("ADMIN", "HR", "MANAGER", "EMPLOYEE"));
    }

    public static Map<String, String> dataContractVersions() {
        return Map.of(
                "user", "v1.0",
                "role", "v1.0");
    }
}

package com.company.service;

import com.company.model.VacationRequest;

public record ManagerDecisionResult(boolean success,
                                    boolean externalUnavailable,
                                    boolean notFound,
                                    String error,
                                    String message,
                                    VacationRequest request) {
    public static ManagerDecisionResult success(VacationRequest request) {
        return new ManagerDecisionResult(true, false, false, null, null, request);
    }

    public static ManagerDecisionResult externalUnavailable(String message) {
        return new ManagerDecisionResult(false, true, false, "external_unavailable", message, null);
    }

    public static ManagerDecisionResult missing() {
        return new ManagerDecisionResult(false, false, true, "not_found", "Request not found", null);
    }

    public static ManagerDecisionResult failure(String error, String message) {
        return new ManagerDecisionResult(false, false, false, error, message, null);
    }
}

package com.company.service;

import com.company.model.VacationRequest;

public record VacationSubmissionResult(boolean success,
                                       boolean externalUnavailable,
                                       String error,
                                       String message,
                                       VacationRequest request) {

    public static VacationSubmissionResult success(VacationRequest request) {
        return new VacationSubmissionResult(true, false, null, null, request);
    }

    public static VacationSubmissionResult externalUnavailable(String message) {
        return new VacationSubmissionResult(false, true, "external_unavailable", message, null);
    }

    public static VacationSubmissionResult failure(String error, String message) {
        return new VacationSubmissionResult(false, false, error, message, null);
    }
}

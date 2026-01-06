package com.company.service;

import com.company.model.VacationRequest;

public record HrProcessingResult(boolean success,
                                 boolean notFound,
                                 boolean externalUnavailable,
                                 String error,
                                 String message,
                                 VacationRequest request) {

    public static HrProcessingResult successResult(VacationRequest request) {
        return new HrProcessingResult(true, false, false, null, null, request);
    }

    public static HrProcessingResult notFoundResult() {
        return new HrProcessingResult(false, true, false, "not_found", "Request not found", null);
    }

    public static HrProcessingResult invalidState(String message) {
        return new HrProcessingResult(false, false, false, "invalid_state", message, null);
    }

    public static HrProcessingResult externalUnavailable(String message) {
        return new HrProcessingResult(false, false, true, "external_unavailable", message, null);
    }

    public static HrProcessingResult failure(String error, String message) {
        return new HrProcessingResult(false, false, false, error, message, null);
    }
}

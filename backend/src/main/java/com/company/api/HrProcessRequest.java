package com.company.api;

import java.util.UUID;

public record HrProcessRequest(UUID requestId, String hrNotes) {
}

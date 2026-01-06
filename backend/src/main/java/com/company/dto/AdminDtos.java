package com.company.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AdminDtos {
    private AdminDtos() {
    }

    public record AdminUserView(UUID id,
                                String username,
                                String displayName,
                                String status,
                                List<String> roles,
                                List<UUID> teamIds,
                                Instant createdAt,
                                Instant updatedAt) {
    }

    public record AdminUserRequest(String username,
                                   String displayName,
                                   String password,
                                   String status,
                                   List<String> roles,
                                   List<UUID> teamIds) {
    }

    public record AdminTeamView(UUID id,
                                String name,
                                String status,
                                List<UUID> memberIds,
                                Instant createdAt) {
    }

    public record AdminTeamRequest(String name,
                                   String status,
                                   List<UUID> memberIds) {
    }

    public record IntegrationConfigDto(UUID id,
                                       String type,
                                       String state,
                                       String endpointUrl,
                                       boolean hasAuthToken,
                                       Instant updatedAt) {
    }

    public record IntegrationConfigRequest(String type,
                                           String endpointUrl,
                                           String authToken) {
    }

    public record HolidayImportResponse(int year,
                                        int imported,
                                        int skipped,
                                        String outcome,
                                        String message) {
    }

    public record HolidayAdminItem(UUID id,
                                   String date,
                                   String name,
                                   String status,
                                   String deprecationReason) {
    }

    public record HolidayDeprecateRequest(String reason) {
    }
}

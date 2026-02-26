package com.example.mondecole_pocket.dto;

import java.time.Instant;

public record RegisterOrganizationResponse(
        Long organizationId,
        String organizationName,
        String slug,
        String invitationCode,
        UserResponse adminUser,
        Instant createdAt,
        String message
) {
    public static RegisterOrganizationResponse of(
            Long organizationId,
            String organizationName,
            String slug,
            String invitationCode,
            UserResponse adminUser,
            Instant createdAt) {
        return new RegisterOrganizationResponse(
                organizationId,
                organizationName,
                slug,
                invitationCode,
                adminUser,
                createdAt,
                "Organization successfully created"
        );
    }
}
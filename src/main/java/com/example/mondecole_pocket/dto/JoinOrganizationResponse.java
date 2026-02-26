package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.UserRole;

import java.time.Instant;

public record JoinOrganizationResponse(
        Long userId,
        String username,
        String email,
        UserRole role,
        OrganizationBasicInfo organization,
        Instant createdAt,
        String message
) {
    public static JoinOrganizationResponse of(
            Long userId,
            String username,
            String email,
            UserRole role,
            OrganizationBasicInfo organization,
            Instant createdAt) {
        return new JoinOrganizationResponse(
                userId,
                username,
                email,
                role,
                organization,
                createdAt,
                "Successfully joined organization"
        );
    }
}
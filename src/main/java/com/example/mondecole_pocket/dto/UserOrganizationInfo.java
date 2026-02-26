package com.example.mondecole_pocket.dto;

public record UserOrganizationInfo(
        Long organizationId,
        String organizationName,
        String organizationSlug,
        String userRole
) {
}
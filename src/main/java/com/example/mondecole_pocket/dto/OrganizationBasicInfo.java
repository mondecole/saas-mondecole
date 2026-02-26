package com.example.mondecole_pocket.dto;

public record OrganizationBasicInfo(
        Long id,
        String name,
        String slug,
        String logoUrl
) {}
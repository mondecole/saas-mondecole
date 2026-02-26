package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.OrganizationType;
import com.example.mondecole_pocket.entity.enums.SubscriptionPlan;

import java.time.LocalDateTime;

public record OrganizationResponse(
        Long id,
        String name,
        String slug,
        String invitationCode,
        String description,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        String logoUrl,
        String website,
        OrganizationType type,
        SubscriptionPlan plan,
        Integer maxUsers,
        Integer maxStorageMB,
        boolean active,
        LocalDateTime createdAt
) {}
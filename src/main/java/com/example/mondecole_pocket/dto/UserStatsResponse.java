package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.UserRole;

public record UserStatsResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String role,
        int enrollmentCount
) {
}

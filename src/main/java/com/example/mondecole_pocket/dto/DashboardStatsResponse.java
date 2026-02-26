package com.example.mondecole_pocket.dto;

public record DashboardStatsResponse(
        long totalUsers,
        long totalTeachers,
        long totalStudents,
        long activeUsers,
        long lockedUsers,
        OrganizationBasicInfo organization
) {}
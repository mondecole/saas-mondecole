package com.example.mondecole_pocket.dto;

import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        String courseTitle,
        Integer progressPercent,
        Boolean completed,
        LocalDateTime enrolledAt,
        LocalDateTime lastAccessedAt
) {}
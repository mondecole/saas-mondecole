package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.CourseLevel;

import java.time.LocalDateTime;

public record CourseListResponse(
        Long id,
        String title,
        String summary,
        String category,
        CourseLevel level,
        Boolean published,
        LocalDateTime createdAt,
        Integer estimatedHours
) {}
package com.example.mondecole_pocket.dto;

public record LessonListResponse(
        Long id,
        String title,
        String type,
        Integer orderIndex,
        Integer durationSeconds
) {}
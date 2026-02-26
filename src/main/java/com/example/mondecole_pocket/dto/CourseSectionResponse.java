package com.example.mondecole_pocket.dto;

public record CourseSectionResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer orderIndex,
        Integer lessonCount
) {}
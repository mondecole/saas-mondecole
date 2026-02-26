package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.CourseLevel;

import java.time.LocalDateTime;

public record CourseDetailResponse(
        Long id,
        Long organizationId,
        Long authorId,
        AuthorInfo author,
        String title,
        String slug,
        String summary,
        String description,
        String category,
        String[] tags,
        CourseLevel level,
        Integer estimatedHours,
        String thumbnailUrl,
        String language,
        Boolean published,
        LocalDateTime publishedAt,
        String objectives,
        String prerequisites,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record AuthorInfo(
            Long id,
            String username,
            String fullName
    ) {}
}
package com.example.mondecole_pocket.dto;

public record LessonDetailResponse(
        Long id,
        Long sectionId,
        String title,
        String description,
        String content,  // ✅ Le contenu complet
        String type,
        Integer orderIndex,
        String externalVideoUrl,
        Integer durationSeconds,
        Boolean downloadable
) {}
package com.example.mondecole_pocket.dto;

public record UpdateLessonRequest(
        String title,
        String description,
        String content,  // ✅ Pour modifier le texte
        String type,
        Integer orderIndex,
        String externalVideoUrl,
        Integer durationSeconds,
        Boolean downloadable
) {}
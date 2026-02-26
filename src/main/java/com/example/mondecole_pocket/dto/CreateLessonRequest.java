package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateLessonRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotBlank(message = "Content is required")
        String content,  // ✅ Le texte de la leçon

        @NotNull(message = "Type is required")
        String type,  // TEXT, VIDEO, QUIZ, etc.

        @NotNull(message = "Order index is required")
        Integer orderIndex,

        String externalVideoUrl,

        Integer durationSeconds,

        Boolean downloadable
) {}
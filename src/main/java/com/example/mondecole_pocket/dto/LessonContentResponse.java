package com.example.mondecole_pocket.dto;

public record LessonContentResponse(
        Long id,
        String title,
        String type,
        String description,

        // Content
        String content,
        String fileUrl,
        String fileName,
        String mimeType,
        Long fileSizeBytes,

        // Video
        Integer durationSeconds,
        String externalVideoUrl,

        // Settings
        Boolean downloadable,

        // Progress
        ProgressInfo progress,

        // Navigation
        NavigationInfo navigation
) {
    public record ProgressInfo(
            Boolean completed,
            Integer progressPercent,
            Integer lastPositionSeconds
    ) {}

    public record NavigationInfo(
            Long previousLessonId,
            Long nextLessonId,
            String sectionTitle,
            Integer lessonNumber,
            Integer totalLessons
    ) {}
}
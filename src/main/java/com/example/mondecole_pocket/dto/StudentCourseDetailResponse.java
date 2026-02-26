package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.CourseLevel;

import java.time.LocalDateTime;
import java.util.List;

public record StudentCourseDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String description,
        String category,
        CourseLevel level,
        Integer estimatedHours,
        String thumbnailUrl,
        String language,

        // Educational
        String objectives,
        String prerequisites,

        // Teacher
        TeacherInfo teacher,

        // Sections & Lessons
        List<SectionInfo> sections,

        // Enrollment
        EnrollmentInfo enrollment,

        // Stats
        Integer totalLessons,
        Integer totalStudents,

        LocalDateTime publishedAt
) {
    public record TeacherInfo(
            Long id,
            String username,
            String fullName
    ) {}

    public record SectionInfo(
            Long id,
            String title,
            String description,
            Integer orderIndex,
            List<LessonInfo> lessons
    ) {}

    public record LessonInfo(
            Long id,
            String title,
            String type,
            Integer orderIndex,
            Integer durationSeconds,
            Boolean completed,
            Integer progressPercent
    ) {}

    public record EnrollmentInfo(
            Long enrollmentId,
            Integer progressPercent,
            Boolean completed,
            LocalDateTime enrolledAt,
            LocalDateTime lastAccessedAt
    ) {}
}
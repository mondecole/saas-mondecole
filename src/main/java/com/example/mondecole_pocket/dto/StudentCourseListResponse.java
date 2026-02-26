package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.CourseLevel;

import java.time.LocalDateTime;

public record StudentCourseListResponse(
        Long id,
        String title,
        String summary,
        String category,
        CourseLevel level,
        Integer estimatedHours,
        String thumbnailUrl,

        // Teacher info
        TeacherInfo teacher,

        // Enrollment info (null if not enrolled)
        EnrollmentInfo enrollment,

        // Stats
        Integer totalStudents,

        LocalDateTime publishedAt
) {
    public record TeacherInfo(
            Long id,
            String username,
            String fullName
    ) {}

    public record EnrollmentInfo(
            Long enrollmentId,
            Integer progressPercent,
            Boolean completed,
            LocalDateTime enrolledAt,
            LocalDateTime lastAccessedAt
    ) {}
}
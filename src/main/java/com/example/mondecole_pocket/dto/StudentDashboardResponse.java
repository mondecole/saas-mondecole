package com.example.mondecole_pocket.dto;

import java.util.List;

public record StudentDashboardResponse(
        Stats stats,
        List<EnrolledCourseInfo> recentCourses,
        List<EnrolledCourseInfo> inProgressCourses
) {
    public record Stats(
            Integer totalEnrolledCourses,
            Integer completedCourses,
            Integer inProgressCourses,
            Integer averageProgress
    ) {}

    public record EnrolledCourseInfo(
            Long courseId,
            String courseTitle,
            String courseThumbnail,
            Integer progressPercent,
            String teacherName,
            java.time.LocalDateTime lastAccessedAt
    ) {}
}
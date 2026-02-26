package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.*;
import com.example.mondecole_pocket.exception.CourseNotFoundException;
import com.example.mondecole_pocket.repository.*;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseSectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;

    /**
     * Browse available courses (catalog)
     */
    @Transactional(readOnly = true)
    public Page<StudentCourseListResponse> browseCourses(Long studentId, Pageable pageable) {
        Long organizationId = TenantContext.getTenantId();

        // Get all published courses
        Page<Course> courses = courseRepository.findByOrganizationIdAndPublishedTrue(organizationId, pageable);

        // Get student enrollments
        List<CourseEnrollment> enrollments = enrollmentRepository.findByOrganizationIdAndStudentId(
                organizationId, studentId, Pageable.unpaged()).getContent();

        Map<Long, CourseEnrollment> enrollmentMap = enrollments.stream()
                .collect(Collectors.toMap(CourseEnrollment::getCourseId, e -> e));

        return courses.map(course -> toStudentCourseListResponse(course, enrollmentMap.get(course.getId())));
    }

    /**
     * Get my enrolled courses
     */
    @Transactional(readOnly = true)
    public Page<StudentCourseListResponse> getMyEnrolledCourses(Long studentId, Boolean completed, Pageable pageable) {
        Long organizationId = TenantContext.getTenantId();

        Page<CourseEnrollment> enrollments;
        if (completed != null && completed) {
            enrollments = enrollmentRepository.findByOrganizationIdAndStudentIdAndCompletedTrue(
                    organizationId, studentId, pageable);
        } else if (completed != null && !completed) {
            enrollments = enrollmentRepository.findByOrganizationIdAndStudentIdAndCompletedFalse(
                    organizationId, studentId, pageable);
        } else {
            enrollments = enrollmentRepository.findByOrganizationIdAndStudentId(
                    organizationId, studentId, pageable);
        }

        List<StudentCourseListResponse> responses = enrollments.getContent().stream()
                .map(enrollment -> {
                    Course course = enrollment.getCourse();
                    return toStudentCourseListResponse(course, enrollment);
                })
                .toList();

        return new PageImpl<>(responses, pageable, enrollments.getTotalElements());
    }

    /**
     * Get course detail for student
     */
    @Transactional(readOnly = true)
    public StudentCourseDetailResponse getCourseDetail(Long courseId, Long studentId) {
        Long organizationId = TenantContext.getTenantId();

        Course course = courseRepository.findByIdAndOrganizationIdAndPublishedTrue(courseId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or not published"));

        // Get enrollment if exists
        CourseEnrollment enrollment = enrollmentRepository
                .findByOrganizationIdAndStudentIdAndCourseId(organizationId, studentId, courseId)
                .orElse(null);

        // Get sections with lessons
        List<CourseSection> sections = sectionRepository
                .findByOrganizationIdAndCourseIdOrderByOrderIndexAsc(organizationId, courseId);

        // Get lesson progress if enrolled
        Map<Long, LessonProgress> progressMap = Map.of();
        if (enrollment != null) {
            List<LessonProgress> progressList = progressRepository
                    .findByStudentAndCourse(organizationId, studentId, courseId);
            progressMap = progressList.stream()
                    .collect(Collectors.toMap(LessonProgress::getLessonId, p -> p));
        }

        // Count total lessons
        int totalLessons = sections.stream()
                .mapToInt(s -> s.getLessons().size())
                .sum();

        // Count total students
        int totalStudents = (int) enrollmentRepository.countByOrganizationIdAndCourseId(organizationId, courseId);

        return toStudentCourseDetailResponse(course, sections, enrollment, progressMap, totalLessons, totalStudents);
    }

    /**
     * Enroll in course
     */
    @Transactional
    public EnrollmentResponse enrollInCourse(Long courseId, Long studentId) {
        Long organizationId = TenantContext.getTenantId();

        // Check if course exists and is published
        Course course = courseRepository.findByIdAndOrganizationIdAndPublishedTrue(courseId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or not published"));

        // Check if already enrolled
        if (enrollmentRepository.existsByOrganizationIdAndStudentIdAndCourseId(
                organizationId, studentId, courseId)) {
            throw new IllegalStateException("Already enrolled in this course");
        }

        // Create enrollment
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .organizationId(organizationId)
                .studentId(studentId)
                .courseId(courseId)
                .progressPercent(0)
                .completed(false)
                .certificateIssued(false)
                .build();

        enrollment = enrollmentRepository.save(enrollment);

        log.info("✅ Student {} enrolled in course {}", studentId, courseId);

        return new EnrollmentResponse(
                enrollment.getId(),
                courseId,
                course.getTitle(),
                0,
                false,
                enrollment.getEnrolledAt(),
                null
        );
    }

    /**
     * Get student dashboard stats
     */
    @Transactional(readOnly = true)
    public StudentDashboardResponse getDashboardStats(Long studentId) {
        Long organizationId = TenantContext.getTenantId();

        // Get all enrollments
        List<CourseEnrollment> enrollments = enrollmentRepository
                .findByOrganizationIdAndStudentId(organizationId, studentId, Pageable.unpaged())
                .getContent();

        int totalEnrolled = enrollments.size();
        int completed = (int) enrollments.stream().filter(CourseEnrollment::getCompleted).count();
        int inProgress = totalEnrolled - completed;

        int avgProgress = enrollments.isEmpty() ? 0 :
                (int) enrollments.stream()
                        .mapToInt(CourseEnrollment::getProgressPercent)
                        .average()
                        .orElse(0);

        // Recent courses (last 5 accessed)
        List<StudentDashboardResponse.EnrolledCourseInfo> recentCourses = enrollments.stream()
                .filter(e -> e.getLastAccessedAt() != null)
                .sorted((a, b) -> b.getLastAccessedAt().compareTo(a.getLastAccessedAt()))
                .limit(5)
                .map(this::toEnrolledCourseInfo)
                .toList();

        // In progress courses
        List<StudentDashboardResponse.EnrolledCourseInfo> inProgressCourses = enrollments.stream()
                .filter(e -> !e.getCompleted() && e.getProgressPercent() > 0)
                .sorted((a, b) -> Integer.compare(b.getProgressPercent(), a.getProgressPercent()))
                .limit(5)
                .map(this::toEnrolledCourseInfo)
                .toList();

        StudentDashboardResponse.Stats stats = new StudentDashboardResponse.Stats(
                totalEnrolled,
                completed,
                inProgress,
                avgProgress
        );

        return new StudentDashboardResponse(stats, recentCourses, inProgressCourses);
    }

    // ════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════

    private StudentCourseListResponse toStudentCourseListResponse(Course course, CourseEnrollment enrollment) {
        StudentCourseListResponse.TeacherInfo teacherInfo = null;
        if (course.getAuthor() != null) {
            User teacher = course.getAuthor();
            teacherInfo = new StudentCourseListResponse.TeacherInfo(
                    teacher.getId(),
                    teacher.getUsername(),
                    teacher.getFullName()
            );
        }

        StudentCourseListResponse.EnrollmentInfo enrollmentInfo = null;
        if (enrollment != null) {
            enrollmentInfo = new StudentCourseListResponse.EnrollmentInfo(
                    enrollment.getId(),
                    enrollment.getProgressPercent(),
                    enrollment.getCompleted(),
                    enrollment.getEnrolledAt(),
                    enrollment.getLastAccessedAt()
            );
        }

        int totalStudents = (int) enrollmentRepository.countByOrganizationIdAndCourseId(
                course.getOrganizationId(), course.getId());

        return new StudentCourseListResponse(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCategory(),
                course.getLevel(),
                course.getEstimatedHours(),
                course.getThumbnailUrl(),
                teacherInfo,
                enrollmentInfo,
                totalStudents,
                course.getPublishedAt()
        );
    }

    private StudentCourseDetailResponse toStudentCourseDetailResponse(
            Course course,
            List<CourseSection> sections,
            CourseEnrollment enrollment,
            Map<Long, LessonProgress> progressMap,
            int totalLessons,
            int totalStudents) {

        StudentCourseDetailResponse.TeacherInfo teacherInfo = null;
        if (course.getAuthor() != null) {
            User teacher = course.getAuthor();
            teacherInfo = new StudentCourseDetailResponse.TeacherInfo(
                    teacher.getId(),
                    teacher.getUsername(),
                    teacher.getFullName()
            );
        }

        List<StudentCourseDetailResponse.SectionInfo> sectionInfos = sections.stream()
                .map(section -> {
                    List<StudentCourseDetailResponse.LessonInfo> lessonInfos = section.getLessons().stream()
                            .map(lesson -> {
                                LessonProgress progress = progressMap.get(lesson.getId());
                                return new StudentCourseDetailResponse.LessonInfo(
                                        lesson.getId(),
                                        lesson.getTitle(),
                                        lesson.getType().name(),
                                        lesson.getOrderIndex(),
                                        lesson.getDurationSeconds(),
                                        progress != null && progress.getCompleted(),
                                        progress != null ? progress.getProgressPercent() : 0
                                );
                            })
                            .toList();

                    return new StudentCourseDetailResponse.SectionInfo(
                            section.getId(),
                            section.getTitle(),
                            section.getDescription(),
                            section.getOrderIndex(),
                            lessonInfos
                    );
                })
                .toList();

        StudentCourseDetailResponse.EnrollmentInfo enrollmentInfo = null;
        if (enrollment != null) {
            enrollmentInfo = new StudentCourseDetailResponse.EnrollmentInfo(
                    enrollment.getId(),
                    enrollment.getProgressPercent(),
                    enrollment.getCompleted(),
                    enrollment.getEnrolledAt(),
                    enrollment.getLastAccessedAt()
            );
        }

        return new StudentCourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getSlug(),
                course.getSummary(),
                course.getDescription(),
                course.getCategory(),
                course.getLevel(),
                course.getEstimatedHours(),
                course.getThumbnailUrl(),
                course.getLanguage(),
                course.getObjectives(),
                course.getPrerequisites(),
                teacherInfo,
                sectionInfos,
                enrollmentInfo,
                totalLessons,
                totalStudents,
                course.getPublishedAt()
        );
    }

    private StudentDashboardResponse.EnrolledCourseInfo toEnrolledCourseInfo(CourseEnrollment enrollment) {
        Course course = enrollment.getCourse();
        String teacherName = course.getAuthor() != null ? course.getAuthor().getFullName() : "Unknown";

        return new StudentDashboardResponse.EnrolledCourseInfo(
                course.getId(),
                course.getTitle(),
                course.getThumbnailUrl(),
                enrollment.getProgressPercent(),
                teacherName,
                enrollment.getLastAccessedAt()
        );
    }
}
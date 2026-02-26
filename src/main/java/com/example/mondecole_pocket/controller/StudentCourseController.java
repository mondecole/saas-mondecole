package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.security.CustomUserDetails;
import com.example.mondecole_pocket.service.StudentCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/student/courses")
@RequiredArgsConstructor
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    /**
     * Browse course catalog
     */
    @GetMapping("/catalog")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Page<StudentCourseListResponse>> browseCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
        Page<StudentCourseListResponse> courses = studentCourseService.browseCourses(
                currentUser.getId(), pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Get my enrolled courses
     */
    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Page<StudentCourseListResponse>> getMyEnrolledCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean completed,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastAccessedAt").descending());
        Page<StudentCourseListResponse> courses = studentCourseService.getMyEnrolledCourses(
                currentUser.getId(), completed, pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Get course detail
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<StudentCourseDetailResponse> getCourseDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        StudentCourseDetailResponse course = studentCourseService.getCourseDetail(
                id, currentUser.getId());

        return ResponseEntity.ok(course);
    }

    /**
     * Enroll in course
     */
    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<EnrollmentResponse> enrollInCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        EnrollmentResponse enrollment = studentCourseService.enrollInCourse(
                id, currentUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    /**
     * Get student dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<StudentDashboardResponse> getDashboard(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        StudentDashboardResponse dashboard = studentCourseService.getDashboardStats(
                currentUser.getId());

        return ResponseEntity.ok(dashboard);
    }
}
package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.security.CustomUserDetails; // ✅ Import
import com.example.mondecole_pocket.service.CourseService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * Get all courses for current teacher
     */
    @GetMapping("/my-courses")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Page<CourseListResponse>> getMyCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean published,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement ici
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CourseListResponse> courses = courseService.getMyCourses(
                currentUser.getId(), published, pageable); // ✅ currentUser.getId()

        return ResponseEntity.ok(courses);
    }

    /**
     * Get course by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDetailResponse> getCourseById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement
    ) {
        CourseDetailResponse course = courseService.getCourseById(id, currentUser.getId());
        return ResponseEntity.ok(course);
    }

    /**
     * Create course
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDetailResponse> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement
    ) {
        // ✅ Passer userId et organizationId au service
        CourseDetailResponse course = courseService.createCourse(
                request,
                currentUser.getId(),
                currentUser.getOrganizationId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    /**
     * Update course
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDetailResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement
    ) {
        CourseDetailResponse course = courseService.updateCourse(id, request, currentUser.getId());
        return ResponseEntity.ok(course);
    }

    /**
     * Delete course
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement
    ) {
        courseService.deleteCourse(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish course
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDetailResponse> publishCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement
    ) {
        CourseDetailResponse course = courseService.publishCourse(id, currentUser.getId());
        return ResponseEntity.ok(course);
    }

    /**
     * Unpublish course
     */
    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseDetailResponse> unpublishCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser // ✅ Changement
    ) {
        CourseDetailResponse course = courseService.unpublishCourse(id, currentUser.getId());
        return ResponseEntity.ok(course);
    }
}
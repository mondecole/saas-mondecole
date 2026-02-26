package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.LessonContentResponse;
import com.example.mondecole_pocket.dto.UpdateLessonProgressRequest;
import com.example.mondecole_pocket.security.CustomUserDetails;
import com.example.mondecole_pocket.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/student/lessons")
@RequiredArgsConstructor
public class StudentLessonController {

    private final LessonService lessonService;

    /**
     * Get lesson content
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<LessonContentResponse> getLessonContent(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LessonContentResponse lesson = lessonService.getLessonContent(id, currentUser.getId());
        return ResponseEntity.ok(lesson);
    }

    /**
     * Update lesson progress
     */
    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<LessonContentResponse> updateProgress(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLessonProgressRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LessonContentResponse lesson = lessonService.updateLessonProgress(
                id, currentUser.getId(), request);

        return ResponseEntity.ok(lesson);
    }

    /**
     * Mark lesson as completed
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Void> markCompleted(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        lessonService.markLessonCompleted(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
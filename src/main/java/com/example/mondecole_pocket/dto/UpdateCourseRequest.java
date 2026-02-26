package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.CourseLevel;
import jakarta.validation.constraints.Size;

public record UpdateCourseRequest(
        @Size(max = 200, message = "Title must be less than 200 characters")
        String title,

        @Size(max = 500, message = "Summary must be less than 500 characters")
        String summary,

        String description,

        @Size(max = 100, message = "Category must be less than 100 characters")
        String category,

        CourseLevel level,

        Integer estimatedHours,

        String objectives,

        String prerequisites
) {}
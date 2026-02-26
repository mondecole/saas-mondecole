package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateLessonProgressRequest(
        @Min(0) @Max(100)
        Integer progressPercent,

        Integer lastPositionSeconds,

        Boolean completed
) {}
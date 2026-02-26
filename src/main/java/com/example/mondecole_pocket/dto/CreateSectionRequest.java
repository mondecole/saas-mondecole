package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSectionRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Order index is required")
        Integer orderIndex
) {}
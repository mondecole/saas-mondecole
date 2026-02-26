package com.example.mondecole_pocket.dto;

public record UpdateSectionRequest(
        String title,
        String description,
        Integer orderIndex
) {}
package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.entity.enums.UserRole;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean active,
        LocalDateTime createdAt,
        @Nullable LocalDateTime lastLoginAt

) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
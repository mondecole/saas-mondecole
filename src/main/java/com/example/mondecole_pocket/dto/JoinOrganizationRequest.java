package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.UserRole;
import jakarta.validation.constraints.*;

public record JoinOrganizationRequest(
        @NotBlank(message = "Invitation code is required")
        String invitationCode,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username can only contain letters, numbers, dashes and underscores")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain uppercase, lowercase, number and special character"
        )
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Role is required")
        UserRole role  // TEACHER or STUDENT
) {
    public void validate() {
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Cannot join as ADMIN. ADMIN users are created during organization setup.");
        }
    }
}
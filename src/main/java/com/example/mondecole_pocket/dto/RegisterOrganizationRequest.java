package com.example.mondecole_pocket.dto;

import com.example.mondecole_pocket.entity.enums.OrganizationType;
import jakarta.validation.constraints.*;

public record RegisterOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(min = 3, max = 200, message = "Name must be between 3 and 200 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        String phone,

        String address,

        String city,

        String postalCode,

        String country,

        String website,

        @NotNull(message = "Organization type is required")
        OrganizationType type,

        String description,

        // Admin user info
        @NotBlank(message = "Admin username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username can only contain letters, numbers, dashes and underscores")
        String adminUsername,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Invalid admin email format")
        String adminEmail,

        @NotBlank(message = "Admin password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain uppercase, lowercase, number and special character"
        )
        String adminPassword,

        @NotBlank(message = "Admin first name is required")
        String adminFirstName,

        @NotBlank(message = "Admin last name is required")
        String adminLastName
) {}
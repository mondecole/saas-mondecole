package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(min = 3, max = 200)
        String name,

        String description,

        @Email
        String email,

        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        String website,
        String logoUrl
) {}
package com.example.mondecole_pocket.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyInvitationCodeRequest(
        @NotBlank(message = "Invitation code is required")
        String invitationCode
) {}
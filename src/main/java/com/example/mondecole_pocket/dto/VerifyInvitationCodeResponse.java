package com.example.mondecole_pocket.dto;

public record VerifyInvitationCodeResponse(
        boolean valid,
        OrganizationBasicInfo organization,
        String message
) {
    public static VerifyInvitationCodeResponse valid(OrganizationBasicInfo organization) {
        return new VerifyInvitationCodeResponse(true, organization, "Valid invitation code");
    }

    public static VerifyInvitationCodeResponse invalid() {
        return new VerifyInvitationCodeResponse(false, null, "Invalid or inactive invitation code");
    }
}
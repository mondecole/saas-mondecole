package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Register a new organization
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterOrganizationResponse> registerOrganization(
            @Valid @RequestBody RegisterOrganizationRequest request) {
        RegisterOrganizationResponse response = organizationService.registerOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verify invitation code
     */
    @PostMapping("/verify-invitation")
    public ResponseEntity<VerifyInvitationCodeResponse> verifyInvitationCode(
            @Valid @RequestBody VerifyInvitationCodeRequest request) {
        VerifyInvitationCodeResponse response =
                organizationService.verifyInvitationCode(request.invitationCode());
        return ResponseEntity.ok(response);
    }

    /**
     * Join organization (teacher or student)
     */
    @PostMapping("/join")
    public ResponseEntity<JoinOrganizationResponse> joinOrganization(
            @Valid @RequestBody JoinOrganizationRequest request) {
        JoinOrganizationResponse response = organizationService.joinOrganization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
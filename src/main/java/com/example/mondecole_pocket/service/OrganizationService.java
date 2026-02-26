package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.Organization;
import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.exception.OrganizationNotFoundException;
import com.example.mondecole_pocket.exception.UserAlreadyExistsException;
import com.example.mondecole_pocket.repository.OrganizationRepository;
import com.example.mondecole_pocket.repository.UserRepository;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register a new organization with its first admin user
     */
    @Transactional
    public RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request) {
        log.info("Registering new organization: {}", request.name());

        // 1. Validate organization doesn't already exist
        if (organizationRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Organization with this name already exists");
        }

        // 2. Create organization
        Organization organization = Organization.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .postalCode(request.postalCode())
                .country(request.country())
                .website(request.website())
                .type(request.type())
                .description(request.description())
                .active(true)
                .build();

        Organization savedOrganization = organizationRepository.save(organization);

        log.info("✅ Organization created: {} (ID: {}, Code: {})",
                savedOrganization.getName(),
                savedOrganization.getId(),
                savedOrganization.getInvitationCode());

        // 3. Create admin user
        if (userRepository.existsByUsernameAndOrganization(
                request.adminUsername(), savedOrganization)) {
            throw new UserAlreadyExistsException("Username already exists in this organization");
        }

        if (userRepository.existsByEmailAndOrganization(
                request.adminEmail(), savedOrganization)) {
            throw new UserAlreadyExistsException("Email already exists in this organization");
        }

        User adminUser = User.builder()
                .organization(savedOrganization)
                .username(request.adminUsername())
                .email(request.adminEmail())
                .passwordHash(passwordEncoder.encode(request.adminPassword()))
                .firstName(request.adminFirstName())
                .lastName(request.adminLastName())
                .role(UserRole.ADMIN)
                .active(true)
                .locked(false)
                .build();

        User savedAdmin = userRepository.save(adminUser);

        log.info("✅ Admin user created: {} for organization {}",
                savedAdmin.getUsername(), savedOrganization.getName());

        // 4. Build response
        UserResponse adminResponse = new UserResponse(
                savedAdmin.getId(),
                savedAdmin.getUsername(),
                savedAdmin.getEmail(),
                savedAdmin.getFirstName(),
                savedAdmin.getLastName(),
                savedAdmin.getRole(),
                savedAdmin.isActive(),
                savedAdmin.getCreatedAt(),
                savedAdmin.getLastLoginAt()
        );

        return RegisterOrganizationResponse.of(
                savedOrganization.getId(),
                savedOrganization.getName(),
                savedOrganization.getSlug(),
                savedOrganization.getInvitationCode(),
                adminResponse,
                savedOrganization.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }

    /**
     * Verify if an invitation code is valid
     */
    @Transactional(readOnly = true)
    public VerifyInvitationCodeResponse verifyInvitationCode(String invitationCode) {
        log.debug("Verifying invitation code: {}", invitationCode);

        return organizationRepository.findByInvitationCodeAndActiveTrue(invitationCode)
                .map(org -> {
                    OrganizationBasicInfo info = new OrganizationBasicInfo(
                            org.getId(),
                            org.getName(),
                            org.getSlug(),
                            org.getLogoUrl()
                    );
                    return VerifyInvitationCodeResponse.valid(info);
                })
                .orElse(VerifyInvitationCodeResponse.invalid());
    }

    /**
     * Join an organization as teacher or student
     */
    @Transactional
    public JoinOrganizationResponse joinOrganization(JoinOrganizationRequest request) {
        log.info("User {} attempting to join organization with code: {}",
                request.username(), request.invitationCode());

        // 1. Validate request
        request.validate();

        // 2. Find organization by invitation code
        Organization organization = organizationRepository
                .findByInvitationCodeAndActiveTrue(request.invitationCode())
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "Invalid or inactive invitation code"));

        // 3. Check if username already exists in this organization
        if (userRepository.existsByUsernameAndOrganization(request.username(), organization)) {
            throw new UserAlreadyExistsException(
                    "Username already exists in this organization");
        }

        // 4. Check if email already exists in this organization
        if (userRepository.existsByEmailAndOrganization(request.email(), organization)) {
            throw new UserAlreadyExistsException(
                    "Email already exists in this organization");
        }

        // 5. Create user
        User user = User.builder()
                .organization(organization)
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .active(true)
                .locked(false)
                .build();

        User savedUser = userRepository.save(user);

        log.info("✅ User {} successfully joined organization {} as {}",
                savedUser.getUsername(),
                organization.getName(),
                savedUser.getRole());

        // 6. Build response
        OrganizationBasicInfo orgInfo = new OrganizationBasicInfo(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getLogoUrl()
        );

        return JoinOrganizationResponse.of(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                orgInfo,
                savedUser.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
    }

    /**
     * Get current organization (from tenant context)
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        Long orgId = TenantContext.getTenantId();
        if (orgId == null) {
            throw new IllegalStateException("No organization context found");
        }

        Organization org = getOrganizationById(orgId);
        return mapToResponse(org);
    }

    /**
     * Get organization by ID
     */
    @Transactional(readOnly = true)
    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException(
                        "Organization not found with id: " + id));
    }

    /**
     * Update organization
     */
    @Transactional
    public OrganizationResponse updateOrganization(UpdateOrganizationRequest request) {
        Long orgId = TenantContext.getTenantId();
        Organization org = getOrganizationById(orgId);

        // Update fields
        if (request.name() != null) org.setName(request.name());
        if (request.description() != null) org.setDescription(request.description());
        if (request.email() != null) org.setEmail(request.email());
        if (request.phone() != null) org.setPhone(request.phone());
        if (request.address() != null) org.setAddress(request.address());
        if (request.city() != null) org.setCity(request.city());
        if (request.postalCode() != null) org.setPostalCode(request.postalCode());
        if (request.country() != null) org.setCountry(request.country());
        if (request.website() != null) org.setWebsite(request.website());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());

        Organization updated = organizationRepository.save(org);
        log.info("✅ Organization updated: {}", updated.getName());

        return mapToResponse(updated);
    }

    /**
     * Regenerate invitation code
     */
    @Transactional
    public String regenerateInvitationCode() {
        Long orgId = TenantContext.getTenantId();
        Organization org = getOrganizationById(orgId);

        String oldCode = org.getInvitationCode();
        String newCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        org.setInvitationCode(org.getName().substring(0, Math.min(3, org.getName().length())).toUpperCase()
                + "-" + newCode);

        organizationRepository.save(org);

        log.info("✅ Invitation code regenerated for {}: {} -> {}",
                org.getName(), oldCode, org.getInvitationCode());

        return org.getInvitationCode();
    }

    private OrganizationResponse mapToResponse(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getInvitationCode(),
                org.getDescription(),
                org.getEmail(),
                org.getPhone(),
                org.getAddress(),
                org.getCity(),
                org.getPostalCode(),
                org.getCountry(),
                org.getLogoUrl(),
                org.getWebsite(),
                org.getType(),
                org.getPlan(),
                org.getMaxUsers(),
                org.getMaxStorageMB(),
                org.isActive(),
                org.getCreatedAt()
        );
    }
}
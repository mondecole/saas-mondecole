package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    boolean existsByIdAndActiveTrue(Long id);

    Optional<Organization> findBySlug(String slug);

    Optional<Organization> findByInvitationCodeAndActiveTrue(String invitationCode);

    boolean existsBySlug(String slug);

    boolean existsByInvitationCode(String invitationCode);

    boolean existsByName(String name);
}
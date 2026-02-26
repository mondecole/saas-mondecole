package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.Course;
import com.example.mondecole_pocket.entity.Organization;
import com.example.mondecole_pocket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    // Find by organization
    Page<Course> findByOrganizationId(Long organizationId, Pageable pageable);

    // Find by author (teacher)
    Page<Course> findByOrganizationIdAndAuthorId(Long organizationId, Long authorId, Pageable pageable);

    // Find by author and published status
    Page<Course> findByOrganizationIdAndAuthorIdAndPublished(
            Long organizationId,
            Long authorId,
            Boolean published,
            Pageable pageable
    );

    // Find published courses in organization
    Page<Course> findByOrganizationIdAndPublishedTrue(Long organizationId, Pageable pageable);

    // Find by id and organization
    Optional<Course> findByIdAndOrganizationId(Long id, Long organizationId);

    // Find by id, organization and author (for teacher access control)
    Optional<Course> findByIdAndOrganizationIdAndAuthorId(Long id, Long organizationId, Long authorId);

    // Check if slug exists in organization
    boolean existsByOrganizationIdAndSlug(Long organizationId, String slug);

    // Count courses by author
    long countByOrganizationIdAndAuthorId(Long organizationId, Long authorId);

    // Find published courses
    Optional<Course> findByIdAndOrganizationIdAndPublishedTrue(Long id, Long organizationId);
}
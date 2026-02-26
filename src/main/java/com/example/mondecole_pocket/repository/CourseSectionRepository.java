package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.CourseSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    List<CourseSection> findByOrganizationIdAndCourseIdOrderByOrderIndexAsc(Long organizationId, Long courseId);

    long countByOrganizationIdAndCourseId(Long organizationId, Long courseId);
    List<CourseSection> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    Optional<CourseSection> findByIdAndOrganizationId(Long id, Long organizationId);
}
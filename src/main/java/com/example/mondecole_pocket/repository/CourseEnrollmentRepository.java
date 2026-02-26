package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.CourseEnrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    // Check if student is enrolled
    boolean existsByOrganizationIdAndStudentIdAndCourseId(Long organizationId, Long studentId, Long courseId);

    // Find enrollment
    Optional<CourseEnrollment> findByOrganizationIdAndStudentIdAndCourseId(Long organizationId, Long studentId, Long courseId);

    // Get all enrollments for student
    Page<CourseEnrollment> findByOrganizationIdAndStudentId(Long organizationId, Long studentId, Pageable pageable);

    // Get active (not completed) enrollments
    Page<CourseEnrollment> findByOrganizationIdAndStudentIdAndCompletedFalse(Long organizationId, Long studentId, Pageable pageable);

    // Get completed enrollments
    Page<CourseEnrollment> findByOrganizationIdAndStudentIdAndCompletedTrue(Long organizationId, Long studentId, Pageable pageable);

    // Count student enrollments
    long countByOrganizationIdAndStudentId(Long organizationId, Long studentId);

    // Count completed courses
    long countByOrganizationIdAndStudentIdAndCompletedTrue(Long organizationId, Long studentId);

    // Count enrollments for a course
    long countByOrganizationIdAndCourseId(Long organizationId, Long courseId);

    int countByStudentId(Long studentId);
}
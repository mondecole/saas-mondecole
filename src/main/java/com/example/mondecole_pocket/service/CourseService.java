package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.Course;
import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.exception.CourseAlreadyPublishedException;
import com.example.mondecole_pocket.exception.CourseNotFoundException;
import com.example.mondecole_pocket.exception.CourseNotPublishedException;
import com.example.mondecole_pocket.repository.CourseRepository;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * Get all courses for current teacher
     */
    @Transactional(readOnly = true)
    public Page<CourseListResponse> getMyCourses(Long authorId, Boolean published, Pageable pageable) {
        Long organizationId = TenantContext.getTenantId();

        Page<Course> courses;
        if (published != null) {
            courses = courseRepository.findByOrganizationIdAndAuthorIdAndPublished(
                    organizationId, authorId, published, pageable);
        } else {
            courses = courseRepository.findByOrganizationIdAndAuthorId(
                    organizationId, authorId, pageable);
        }

        return courses.map(this::toCourseListResponse);
    }

    /**
     * Get course by ID (with author verification)
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseById(Long courseId, Long authorId) {
        Long organizationId = TenantContext.getTenantId();

        Course course = courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        courseId, organizationId, authorId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));

        return toCourseDetailResponse(course);
    }

    /**
     * Create new course
     * ✅ MODIFIÉ : Accepte authorId et organizationId au lieu de User
     */
    @Transactional
    public CourseDetailResponse createCourse(
            CreateCourseRequest request,
            Long authorId,              // ✅ Nouveau paramètre
            Long organizationId         // ✅ Nouveau paramètre
    ) {
        // ✅ Vérifier la cohérence avec le TenantContext
        Long contextOrgId = TenantContext.getTenantId();
        if (!organizationId.equals(contextOrgId)) {
            log.error("🔴 Organization mismatch: param={}, context={}", organizationId, contextOrgId);
            throw new IllegalStateException("Organization mismatch");
        }

        // Generate unique slug
        String slug = generateUniqueSlug(request.title(), organizationId);

        Course course = Course.builder()
                .organizationId(organizationId)
                .authorId(authorId)  // ✅ Utiliser le paramètre
                .title(request.title())
                .slug(slug)
                .summary(request.summary())
                .description(request.description())
                .category(request.category())
                .level(request.level())
                .estimatedHours(request.estimatedHours())
                .objectives(request.objectives())
                .prerequisites(request.prerequisites())
                .language("fr")
                .published(false)
                .active(true)
                .build();

        course = courseRepository.save(course);

        log.info("✅ Course created: id={}, title={}, authorId={}, org={}",
                course.getId(), course.getTitle(), authorId, organizationId);

        return toCourseDetailResponse(course);
    }

    /**
     * Update course
     */
    @Transactional
    public CourseDetailResponse updateCourse(Long courseId, UpdateCourseRequest request, Long authorId) {
        Long organizationId = TenantContext.getTenantId();

        Course course = courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        courseId, organizationId, authorId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));

        // Update fields if provided
        if (request.title() != null && !request.title().equals(course.getTitle())) {
            course.setTitle(request.title());
            // Regenerate slug if title changed
            course.setSlug(generateUniqueSlug(request.title(), organizationId));
        }

        if (request.summary() != null) {
            course.setSummary(request.summary());
        }

        if (request.description() != null) {
            course.setDescription(request.description());
        }

        if (request.category() != null) {
            course.setCategory(request.category());
        }

        if (request.level() != null) {
            course.setLevel(request.level());
        }

        if (request.estimatedHours() != null) {
            course.setEstimatedHours(request.estimatedHours());
        }

        if (request.objectives() != null) {
            course.setObjectives(request.objectives());
        }

        if (request.prerequisites() != null) {
            course.setPrerequisites(request.prerequisites());
        }

        course = courseRepository.save(course);

        log.info("✅ Course updated: id={}, title={}", course.getId(), course.getTitle());

        return toCourseDetailResponse(course);
    }

    /**
     * Delete course
     */
    @Transactional
    public void deleteCourse(Long courseId, Long authorId) {
        Long organizationId = TenantContext.getTenantId();

        Course course = courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        courseId, organizationId, authorId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));

        courseRepository.delete(course);

        log.info("✅ Course deleted: id={}, title={}", course.getId(), course.getTitle());
    }


    @Transactional
    public CourseDetailResponse publishCourse(Long courseId, Long authorId) {
        Long organizationId = TenantContext.getTenantId();

        Course course = courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        courseId, organizationId, authorId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));

        if (course.getPublished()) {
            throw new CourseAlreadyPublishedException("Course is already published"); // ← FIX
        }

        course.setPublished(true);
        course.setPublishedAt(LocalDateTime.now());
        course = courseRepository.save(course);

        log.info("✅ Course published: id={}, title={}", course.getId(), course.getTitle());

        return toCourseDetailResponse(course);
    }

    @Transactional
    public CourseDetailResponse unpublishCourse(Long courseId, Long authorId) {
        Long organizationId = TenantContext.getTenantId();

        Course course = courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        courseId, organizationId, authorId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found"));

        if (!course.getPublished()) {
            throw new CourseNotPublishedException("Course is not published"); // ← FIX
        }

        course.setPublished(false);
        course = courseRepository.save(course);

        log.info("✅ Course unpublished: id={}, title={}", course.getId(), course.getTitle());

        return toCourseDetailResponse(course);
    }

    // ════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════

    private String generateUniqueSlug(String title, Long organizationId) {
        String baseSlug = slugify(title);
        String slug = baseSlug;
        int counter = 1;

        while (courseRepository.existsByOrganizationIdAndSlug(organizationId, slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "course";
        }

        String slug = text.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        return slug.isEmpty() ? "course" : slug;
    }

    private CourseListResponse toCourseListResponse(Course course) {
        return new CourseListResponse(
                course.getId(),
                course.getTitle(),
                course.getSummary(),
                course.getCategory(),
                course.getLevel(),
                course.getPublished(),
                course.getCreatedAt(),
                course.getEstimatedHours()
        );
    }

    private CourseDetailResponse toCourseDetailResponse(Course course) {
        CourseDetailResponse.AuthorInfo authorInfo = null;
        if (course.getAuthor() != null) {
            User author = course.getAuthor();
            authorInfo = new CourseDetailResponse.AuthorInfo(
                    author.getId(),
                    author.getUsername(),
                    author.getFullName()
            );
        }

        return new CourseDetailResponse(
                course.getId(),
                course.getOrganizationId(),
                course.getAuthorId(),
                authorInfo,
                course.getTitle(),
                course.getSlug(),
                course.getSummary(),
                course.getDescription(),
                course.getCategory(),
                course.getTags(),
                course.getLevel(),
                course.getEstimatedHours(),
                course.getThumbnailUrl(),
                course.getLanguage(),
                course.getPublished(),
                course.getPublishedAt(),
                course.getObjectives(),
                course.getPrerequisites(),
                course.getActive(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
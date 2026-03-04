package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.Course;
import com.example.mondecole_pocket.entity.enums.CourseLevel;
import com.example.mondecole_pocket.exception.CourseAlreadyPublishedException;
import com.example.mondecole_pocket.exception.CourseNotFoundException;
import com.example.mondecole_pocket.exception.CourseNotPublishedException;
import com.example.mondecole_pocket.repository.CourseRepository;
import com.example.mondecole_pocket.security.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de CourseService.
 *
 * Pas de Spring, pas de BDD, pas de Docker.
 * TenantContext (ThreadLocal) est alimenté manuellement dans @BeforeEach
 * et nettoyé dans @AfterEach — ce qui est correct ici car il n'y a pas
 * de filter chain Spring MVC (contrairement aux tests d'intégration MockMvc).
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    private static final Long ORG_ID    = 1L;
    private static final Long AUTHOR_ID = 10L;
    private static final Long COURSE_ID = 100L;

    // TenantContext est un ThreadLocal global — on le gère manuellement ici
    @BeforeEach
    void setTenant() {
        TenantContext.setTenantId(ORG_ID);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    // ══════════════════════════════════════════════════════════════
    // getMyCourses
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getMyCourses — sans filtre published → retourne tous les cours mappés")
    void getMyCourses_shouldReturnAllCourses_whenPublishedFilterIsNull() {
        Course c1 = buildCourse(1L, "Math", false);
        Course c2 = buildCourse(2L, "Science", true);
        Pageable pageable = PageRequest.of(0, 10);

        when(courseRepository.findByOrganizationIdAndAuthorId(ORG_ID, AUTHOR_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(c1, c2)));

        Page<CourseListResponse> result = courseService.getMyCourses(AUTHOR_ID, null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).isEqualTo("Math");
        assertThat(result.getContent().get(1).title()).isEqualTo("Science");

        verify(courseRepository).findByOrganizationIdAndAuthorId(ORG_ID, AUTHOR_ID, pageable);
        verify(courseRepository, never())
                .findByOrganizationIdAndAuthorIdAndPublished(any(), any(), any(), any());
    }

    @Test
    @DisplayName("getMyCourses — avec published=true → filtre uniquement les publiés")
    void getMyCourses_shouldFilterByPublished_whenPublishedIsTrue() {
        Course published = buildCourse(1L, "Math", true);
        Pageable pageable = PageRequest.of(0, 10);

        when(courseRepository.findByOrganizationIdAndAuthorIdAndPublished(ORG_ID, AUTHOR_ID, true, pageable))
                .thenReturn(new PageImpl<>(List.of(published)));

        Page<CourseListResponse> result = courseService.getMyCourses(AUTHOR_ID, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Math");

        verify(courseRepository)
                .findByOrganizationIdAndAuthorIdAndPublished(ORG_ID, AUTHOR_ID, true, pageable);
        verify(courseRepository, never())
                .findByOrganizationIdAndAuthorId(any(), any(), any());
    }

    // ══════════════════════════════════════════════════════════════
    // getCourseById
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getCourseById — cours trouvé → retourne le détail")
    void getCourseById_shouldReturnDetail_whenFound() {
        Course course = buildCourse(COURSE_ID, "Physique", false);

        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.of(course));

        CourseDetailResponse result = courseService.getCourseById(COURSE_ID, AUTHOR_ID);

        assertThat(result.title()).isEqualTo("Physique");
        assertThat(result.id()).isEqualTo(COURSE_ID);
    }

    @Test
    @DisplayName("getCourseById — cours introuvable → CourseNotFoundException")
    void getCourseById_shouldThrow_whenNotFound() {
        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourseById(COURSE_ID, AUTHOR_ID))
                .isInstanceOf(CourseNotFoundException.class)
                .hasMessage("Course not found");
    }

    // ══════════════════════════════════════════════════════════════
    // createCourse
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createCourse — données valides → sauvegarde + retourne la réponse")
    void createCourse_shouldSave_andReturnResponse() {
        CreateCourseRequest req = new CreateCourseRequest(
                "Mathématiques", "Résumé", "Description longue",
                "Sciences", CourseLevel.BEGINNER, 10, "Calcul", null
        );

        when(courseRepository.existsByOrganizationIdAndSlug(eq(ORG_ID), anyString()))
                .thenReturn(false);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        when(courseRepository.save(captor.capture()))
                .thenAnswer(inv -> {
                    Course c = inv.getArgument(0);
                    c.setId(COURSE_ID);
                    return c;
                });

        CourseDetailResponse result = courseService.createCourse(req, AUTHOR_ID, ORG_ID);

        Course saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Mathématiques");
        assertThat(saved.getOrganizationId()).isEqualTo(ORG_ID);
        assertThat(saved.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(saved.getPublished()).isFalse();
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getSlug()).isEqualTo("mathematiques");

        assertThat(result.title()).isEqualTo("Mathématiques");
        assertThat(result.id()).isEqualTo(COURSE_ID);
    }

    @Test
    @DisplayName("createCourse — organizationId ne correspond pas au TenantContext → IllegalStateException")
    void createCourse_shouldThrow_whenOrgMismatch() {
        CreateCourseRequest req = new CreateCourseRequest(
                "Cours", null, null, null, null, null, null, null
        );

        // TenantContext = ORG_ID(1L), mais on passe 99L → mismatch
        assertThatThrownBy(() -> courseService.createCourse(req, AUTHOR_ID, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Organization mismatch");

        verifyNoInteractions(courseRepository);
    }

    // ══════════════════════════════════════════════════════════════
    // publishCourse / unpublishCourse
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("publishCourse — cours non publié → publie et retourne le détail")
    void publishCourse_shouldPublish_whenNotAlreadyPublished() {
        Course course = buildCourse(COURSE_ID, "Chimie", false);

        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        CourseDetailResponse result = courseService.publishCourse(COURSE_ID, AUTHOR_ID);

        assertThat(course.getPublished()).isTrue();
        assertThat(course.getPublishedAt()).isNotNull();
        assertThat(result.title()).isEqualTo("Chimie");
    }

    @Test
    @DisplayName("publishCourse — cours déjà publié → CourseAlreadyPublishedException")
    void publishCourse_shouldThrow_whenAlreadyPublished() {
        Course course = buildCourse(COURSE_ID, "Chimie", true);

        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.publishCourse(COURSE_ID, AUTHOR_ID))
                .isInstanceOf(CourseAlreadyPublishedException.class)  // ← FIX
                .hasMessage("Course is already published");
    }

    @Test
    @DisplayName("unpublishCourse — cours publié → dépublie")
    void unpublishCourse_shouldUnpublish_whenPublished() {
        Course course = buildCourse(COURSE_ID, "Biologie", true);

        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        courseService.unpublishCourse(COURSE_ID, AUTHOR_ID);

        assertThat(course.getPublished()).isFalse();
    }

    @Test
    @DisplayName("unpublishCourse — cours non publié → CourseNotPublishedException")
    void unpublishCourse_shouldThrow_whenNotPublished() {
        Course course = buildCourse(COURSE_ID, "Biologie", false);

        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.unpublishCourse(COURSE_ID, AUTHOR_ID))
                .isInstanceOf(CourseNotPublishedException.class)  // ← FIX
                .hasMessage("Course is not published");
    }

    // ══════════════════════════════════════════════════════════════
    // deleteCourse
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteCourse — cours trouvé → supprime")
    void deleteCourse_shouldDelete_whenFound() {
        Course course = buildCourse(COURSE_ID, "Histoire", false);

        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.of(course));

        courseService.deleteCourse(COURSE_ID, AUTHOR_ID);

        verify(courseRepository).delete(course);
    }

    @Test
    @DisplayName("deleteCourse — cours introuvable → CourseNotFoundException")
    void deleteCourse_shouldThrow_whenNotFound() {
        when(courseRepository.findByIdAndOrganizationIdAndAuthorId(COURSE_ID, ORG_ID, AUTHOR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.deleteCourse(COURSE_ID, AUTHOR_ID))
                .isInstanceOf(CourseNotFoundException.class);

        verify(courseRepository, never()).delete(any());
    }

    // ══════════════════════════════════════════════════════════════
    // HELPER
    // ══════════════════════════════════════════════════════════════

    private Course buildCourse(Long id, String title, boolean published) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        c.setSlug(title.toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        c.setOrganizationId(ORG_ID);
        c.setAuthorId(AUTHOR_ID);
        c.setLevel(CourseLevel.BEGINNER);
        c.setPublished(published);
        c.setActive(true);
        return c;
    }
}
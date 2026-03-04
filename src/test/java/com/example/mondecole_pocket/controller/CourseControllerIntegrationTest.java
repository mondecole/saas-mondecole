package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.config.AbstractIntegrationTest;
import com.example.mondecole_pocket.entity.Course;
import com.example.mondecole_pocket.entity.Organization;
import com.example.mondecole_pocket.entity.User;
import com.example.mondecole_pocket.entity.enums.OrganizationType;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.repository.CourseRepository;
import com.example.mondecole_pocket.repository.OrganizationRepository;
import com.example.mondecole_pocket.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CourseControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/courses";

    @Autowired private CourseRepository       courseRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserRepository         userRepository;
    @Autowired private ObjectMapper           objectMapper;

    private Long orgId;
    private Long otherOrgId;
    private Long teacherId;
    private Long otherUserId;   // ← FIX : vrai user de l'autre org
    private Long seedCourseId;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        // Org principale
        Organization org = new Organization();
        org.setName("Ecole Test");
        org.setType(OrganizationType.UNIVERSITY);
        org.setActive(true);
        org = organizationRepository.save(org);
        orgId = org.getId();

        // Autre org
        Organization otherOrg = new Organization();
        otherOrg.setName("Autre Ecole");
        otherOrg.setType(OrganizationType.UNIVERSITY);
        otherOrg.setActive(true);
        otherOrg = organizationRepository.save(otherOrg);
        otherOrgId = otherOrg.getId();

        // Teacher de l'org principale
        User teacher = new User();
        teacher.setUsername("teacher@test.com");
        teacher.setOrganization(org);
        teacher.setRole(UserRole.TEACHER);
        teacher.setPasswordHash("irrelevant");
        teacher = userRepository.save(teacher);
        teacherId = teacher.getId();

        // ← FIX : vrai user en base pour l'autre org (sinon JwtFilter rejette → 401)
        User otherUser = new User();
        otherUser.setUsername("other@test.com");
        otherUser.setOrganization(otherOrg);
        otherUser.setRole(UserRole.TEACHER);
        otherUser.setPasswordHash("irrelevant");
        otherUser = userRepository.save(otherUser);
        otherUserId = otherUser.getId();

        // Cours seed
        Course seed = new Course();
        seed.setOrganizationId(orgId);
        seed.setAuthorId(teacherId);
        seed.setTitle("Cours seed");
        seed.setSlug("cours-seed");
        seed.setPublished(false);
        seed.setActive(true);
        seed = courseRepository.save(seed);
        seedCourseId = seed.getId();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @Override
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(teacherId, orgId, "teacher@test.com", "TEACHER");
        return request.header("Authorization", "Bearer " + token);
    }

    @Override
    protected MockHttpServletRequestBuilder withOtherAuth(MockHttpServletRequestBuilder request) {
        // ← FIX : utilise le vrai userId du user de l'autre org
        String token = generateToken(otherUserId, otherOrgId, "other@test.com", "TEACHER");
        return request.header("Authorization", "Bearer " + token);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Sécurité — 401 sans token
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /my-courses → 401 si pas de header Authorization")
    void getMyCourses_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get(BASE + "/my-courses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /my-courses → 401 si JWT invalide")
    void getMyCourses_shouldReturn401_whenInvalidToken() throws Exception {
        mockMvc.perform(get(BASE + "/my-courses")
                        .header("Authorization", "Bearer this.is.not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET /my-courses
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /my-courses → 200 + liste des cours de l'auteur (seed = 1)")
    void getMyCourses_shouldReturn200_withSeedCourse() throws Exception {
        mockMvc.perform(withAuth(get(BASE + "/my-courses")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Cours seed"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /my-courses?published=true → ne retourne que les cours publiés")
    void getMyCourses_shouldFilterPublished() throws Exception {
        mockMvc.perform(withAuth(get(BASE + "/my-courses").param("published", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("GET /my-courses → isolation tenant : autre tenant ne voit pas les cours du premier")
    void getMyCourses_shouldIsolateTenants() throws Exception {
        mockMvc.perform(withOtherAuth(get(BASE + "/my-courses")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    // ══════════════════════════════════════════════════════════════════════
    // GET /{id}
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /{id} → 200 avec le cours seed")
    void getCourseById_shouldReturn200_whenOwnedByUser() throws Exception {
        mockMvc.perform(withAuth(get(BASE + "/" + seedCourseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seedCourseId))
                .andExpect(jsonPath("$.title").value("Cours seed"));
    }

    @Test
    @DisplayName("GET /{id} → 404 quand le cours appartient à un autre tenant")
    void getCourseById_shouldReturn404_whenOtherTenant() throws Exception {
        mockMvc.perform(withOtherAuth(get(BASE + "/" + seedCourseId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id} → 404 si l'id n'existe pas")
    void getCourseById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(withAuth(get(BASE + "/99999")))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════════════════════════════
    // POST /
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST / → 201 + cours créé + slug généré automatiquement")
    void createCourse_shouldReturn201_andPersist() throws Exception {
        String body = """
            {
              "title": "Introduction au Java",
              "summary": "Un cours pour débutants",
              "description": "Description complète",
              "category": "Informatique",
              "level": "BEGINNER",
              "estimatedHours": 8,
              "objectives": "Comprendre la POO",
              "prerequisites": null
            }
            """;

        mockMvc.perform(withAuth(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Introduction au Java"))
                .andExpect(jsonPath("$.slug").value("introduction-au-java"))
                .andExpect(jsonPath("$.published").value(false));

        assertThat(courseRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST / → slug avec suffixe numérique si doublon de titre")
    void createCourse_shouldAppendSuffix_whenSlugAlreadyExists() throws Exception {
        String body = """
            {
              "title": "Cours seed",
              "summary": null,
              "description": null,
              "category": "Divers",
              "level": "BEGINNER",
              "estimatedHours": 1,
              "objectives": null,
              "prerequisites": null
            }
            """;

        mockMvc.perform(withAuth(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("cours-seed-1"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // PUT /{id}
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PUT /{id} → 200 et champs mis à jour")
    void updateCourse_shouldReturn200_andUpdateFields() throws Exception {
        String body = """
            {
              "title": "Nouveau titre",
              "summary": "Nouveau résumé",
              "description": null,
              "category": "Maths",
              "level": "ADVANCED",
              "estimatedHours": 20,
              "objectives": "Objectif 1",
              "prerequisites": "Prérequis 1"
            }
            """;

        mockMvc.perform(withAuth(put(BASE + "/" + seedCourseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nouveau titre"))
                .andExpect(jsonPath("$.slug").value("nouveau-titre"))
                .andExpect(jsonPath("$.level").value("ADVANCED"));

        Course updated = courseRepository.findById(seedCourseId).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Nouveau titre");
    }

    @Test
    @DisplayName("PUT /{id} → 404 si le cours appartient à un autre tenant")
    void updateCourse_shouldReturn404_whenOtherTenant() throws Exception {
        String body = """
            { "title": "Hack", "summary": null, "description": null,
              "category": null, "level": null, "estimatedHours": null,
              "objectives": null, "prerequisites": null }
            """;

        mockMvc.perform(withOtherAuth(put(BASE + "/" + seedCourseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════════════════════════════════
    // DELETE /{id}
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /{id} → 204 puis DB ne contient plus le cours")
    void deleteCourse_shouldReturn204_andRemoveFromDb() throws Exception {
        mockMvc.perform(withAuth(delete(BASE + "/" + seedCourseId)))
                .andExpect(status().isNoContent());

        assertThat(courseRepository.findById(seedCourseId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /{id} → 404 si autre tenant tente de supprimer")
    void deleteCourse_shouldReturn404_whenOtherTenant() throws Exception {
        mockMvc.perform(withOtherAuth(delete(BASE + "/" + seedCourseId)))
                .andExpect(status().isNotFound());

        assertThat(courseRepository.findById(seedCourseId)).isPresent();
    }

    // ══════════════════════════════════════════════════════════════════════
    // PATCH /{id}/publish
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /{id}/publish → 200 et published=true")
    void publishCourse_shouldReturn200_andSetPublishedTrue() throws Exception {
        mockMvc.perform(withAuth(patch(BASE + "/" + seedCourseId + "/publish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.publishedAt").exists());
    }

    @Test
    @DisplayName("PATCH /{id}/publish → 400 si déjà publié")
    void publishCourse_shouldReturn400_whenAlreadyPublished() throws Exception {
        mockMvc.perform(withAuth(patch(BASE + "/" + seedCourseId + "/publish")))
                .andExpect(status().isOk());

        mockMvc.perform(withAuth(patch(BASE + "/" + seedCourseId + "/publish")))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════════════════════════════════
    // PATCH /{id}/unpublish
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("PATCH /{id}/unpublish → 200 et published=false")
    void unpublishCourse_shouldReturn200_andSetPublishedFalse() throws Exception {
        mockMvc.perform(withAuth(patch(BASE + "/" + seedCourseId + "/publish")))
                .andExpect(status().isOk());

        mockMvc.perform(withAuth(patch(BASE + "/" + seedCourseId + "/unpublish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    @DisplayName("PATCH /{id}/unpublish → 400 si déjà non publié")
    void unpublishCourse_shouldReturn400_whenNotPublished() throws Exception {
        mockMvc.perform(withAuth(patch(BASE + "/" + seedCourseId + "/unpublish")))
                .andExpect(status().isBadRequest());
    }
}
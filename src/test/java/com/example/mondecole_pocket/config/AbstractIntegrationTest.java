package com.example.mondecole_pocket.config;

import com.example.mondecole_pocket.security.TenantContext;
import com.example.mondecole_pocket.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

/**
 * Classe de base pour tous les tests d'intégration.
 *
 * - Démarre un vrai PostgreSQL via Testcontainers (Docker)
 * - Le container est partagé entre toutes les classes (static) pour
 *   éviter de redémarrer Docker à chaque classe → gain de temps
 * - Flyway migre le schéma automatiquement au démarrage du contexte Spring
 * - MockMvc teste les endpoints HTTP sans démarrer un vrai serveur
 *
 * IMPORTANT — TenantFilter + JWT :
 * TenantFilter lit l'organizationId depuis le JWT Bearer token.
 * Les requêtes MockMvc s'exécutent dans un thread séparé (RANDOM_PORT),
 * donc le filtre tourne normalement et alimente TenantContext via ThreadLocal.
 * → Utiliser le helper withAuth() sur chaque requête MockMvc.
 * → Ne jamais alimenter TenantContext manuellement dans @BeforeEach.
 *
 * IMPORTANT — Organisation en base :
 * TenantFilter appelle organizationRepository.existsByIdAndActiveTrue(orgId).
 * Chaque test d'intégration doit donc persister une Organisation active
 * avec TEST_ORG_ID dans @BeforeEach avant d'appeler withAuth().
 *
 * IMPORTANT — Pas de @Transactional sur les classes de test :
 * Avec RANDOM_PORT, chaque requête HTTP commit réellement en base dans
 * son propre thread. Le nettoyage se fait via deleteAll() dans @BeforeEach,
 * ce qui évite tous les problèmes de merge/rollback Hibernate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // ← MOCK → RANDOM_PORT
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // static = un seul container PostgreSQL pour toute la suite de tests.
    // Sans static, Docker redémarre un container par classe → lent.
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("mondecole_test")
                    .withUsername("postgres")
                    .withPassword("secret");

    /**
     * Appelé AVANT le démarrage du contexte Spring.
     * Injecte dynamiquement l'URL du container Testcontainers dans les properties,
     * ce qui override application-test.yml.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url",          postgres::getJdbcUrl);
        registry.add("spring.flyway.user",         postgres::getUsername);
        registry.add("spring.flyway.password",     postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    // IDs fixes et lisibles, reproductibles d'un run à l'autre
    protected static final Long TEST_ORG_ID   = 1L;
    protected static final Long TEST_USER_ID  = 10L;
    protected static final Long OTHER_ORG_ID  = 2L;
    protected static final Long OTHER_USER_ID = 20L;

    protected String generateToken(Long userId, Long orgId, String username, String role) {
        return jwtService.generateToken(
                userId,
                orgId,
                username,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(TEST_USER_ID, TEST_ORG_ID, "teacher@test.com", "TEACHER");
        return request.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder withOtherAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(OTHER_USER_ID, OTHER_ORG_ID, "other@test.com", "TEACHER");
        return request.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder withAdminAuth(MockHttpServletRequestBuilder request) {
        String token = generateToken(TEST_USER_ID, TEST_ORG_ID, "admin@test.com", "ADMIN");
        return request.header("Authorization", "Bearer " + token);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }
}